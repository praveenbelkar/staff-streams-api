package education.nsw.icc.streams;

import static education.nsw.icc.streams.Constants.ARCHIVED_STATUS;
import static education.nsw.icc.streams.util.Schemas.Topics.STAFF_FINAL_TOPIC;
import static education.nsw.icc.streams.util.Schemas.Topics.STAFF_SOURCE_TOPIC;

import education.nsw.icc.streams.util.DefaultId;
import education.nsw.icc.streams.util.Schemas;
import education.nsw.streams.avro.api.Employment;
import education.nsw.streams.avro.api.StaffSink;
import education.nsw.streams.avro.api.StaffSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.processor.StateRestoreListener;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StaffProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(StaffProcessor.class);

  @ConfigProperty(name = "staff.deduplicate.state.store")
  String STAFF_DEDUPLICATE_STATE_STORE;

  @ConfigProperty(name = "staff.aggregate.state.store")
  String STAFF_AGGREGATE_STATE_STORE;

  @Inject Schemas schemas;

  @Produces
  public StateRestoreListener buildStateRestoreListener() {
    return new StateRestoreListener() {
      @Override
      public void onRestoreStart(
          TopicPartition topicPartition, String storeName, long startingOffset, long endingOffset) {
        LOG.info(
            "Starting restore of "
                + (endingOffset - startingOffset)
                + " records for "
                + storeName
                + " : partition "
                + topicPartition.partition());
      }

      @Override
      public void onBatchRestored(
          TopicPartition topicPartition, String storeName, long batchEndOffset, long numRestored) {}

      @Override
      public void onRestoreEnd(
          TopicPartition topicPartition, String storeName, long totalRestored) {
        LOG.info(
            "Completed restore of "
                + totalRestored
                + " records for "
                + storeName
                + " : partition "
                + topicPartition.partition());
      }
    };
  }

  @Produces
  public Topology buildTopology() {
    StreamsBuilder builder = new StreamsBuilder();
    // create store
    schemas.configureSerdes();

    StoreBuilder<KeyValueStore<String, StaffSource>> keyValueStoreBuilder =
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(STAFF_DEDUPLICATE_STATE_STORE),
            Serdes.String(),
            STAFF_SOURCE_TOPIC.valueSerde());

    // register store
    builder.addStateStore(keyValueStoreBuilder);

    builder.stream(
            STAFF_SOURCE_TOPIC.name(),
            Consumed.with(STAFF_SOURCE_TOPIC.keySerde(), STAFF_SOURCE_TOPIC.valueSerde()))
        .transform(
            () -> new StaffTransformer(STAFF_DEDUPLICATE_STATE_STORE),
            STAFF_DEDUPLICATE_STATE_STORE)
        .mapValues(convertStaffSourceToStaffSink())
        .groupByKey(Grouped.with(Schemas.DEFAULT_ID_SERDE, STAFF_FINAL_TOPIC.valueSerde()))
        .aggregate(
            () -> {
              StaffSink sink = new StaffSink();
              sink.setStaffEmployment(new ArrayList<>());
              return sink;
            },
            aggregateSchoolsOnKeyStaffId(),
            Materialized.<DefaultId, StaffSink, KeyValueStore<Bytes, byte[]>>as(
                    STAFF_AGGREGATE_STATE_STORE)
                .withKeySerde(Schemas.DEFAULT_ID_SERDE)
                .withValueSerde(STAFF_FINAL_TOPIC.valueSerde()))
        .toStream()
        .filter((key, value) -> value != null) // ensure sink can never delete
        // .print(Printed.<String, StaffSink>toSysOut().withLabel("Final topic"));
        .to(
            STAFF_FINAL_TOPIC.name(),
            Produced.with(STAFF_FINAL_TOPIC.keySerde(), STAFF_FINAL_TOPIC.valueSerde()));

    return builder.build();
  }

  private Aggregator<DefaultId, StaffSink, StaffSink> aggregateSchoolsOnKeyStaffId() {
    return (key, value, aggregation) -> {
      switch (StaffUtil.recordType(value)) {
        case DELETE_EMPLOYEE:
          aggregation.setStaffStatus(ARCHIVED_STATUS);
          break;
        case DELETE_EMPLOYMENT:
          Employment employmentToBeDeleted = value.getStaffEmployment().get(0);
          LOG.debug(
              "employmentToBeDeleted: {}:{}:{}:{}",
              key,
              employmentToBeDeleted.getStaffSchoolCode(),
              employmentToBeDeleted.getSchoolStaffIdentifier(),
              employmentToBeDeleted.getStaffFunctionalUnitCode());

          if (CollectionUtils.isNotEmpty(aggregation.getStaffEmployment())) {
            List<Employment> tempList = new ArrayList<>(aggregation.getStaffEmployment());
            tempList.removeIf(employment -> StaffUtil.isSame(employment, employmentToBeDeleted));
            aggregation.setStaffEmployment(tempList);
          }
          break;
        case ACTIVE:
          // merge employee data
          copyProperties(value, aggregation);

          // merge employment data
          if (CollectionUtils.isNotEmpty(value.getStaffEmployment())) {
            Employment employmentOfValue = value.getStaffEmployment().get(0);
            Map<String, Employment> map = employmentListAsMap(aggregation.getStaffEmployment());

            map.put(StaffUtil.makeStaffEmploymentKey(employmentOfValue), employmentOfValue);
            aggregation.setStaffEmployment(new ArrayList<>(map.values()));
          }
        default:
      }

      // send tombstone if invalid aggregation (will be filtered in streams)
      return StringUtils.isBlank(aggregation.getStaffIdentifier()) ? null : aggregation;
    };
  }

  private Map<String, Employment> employmentListAsMap(List<Employment> employmentList) {
    Map<String, Employment> map = new HashMap<>();
    if (employmentList != null) {
      for (Employment e : employmentList) {
        map.put(StaffUtil.makeStaffEmploymentKey(e), e);
      }
    }
    return map;
  }

  private void copyProperties(final StaffSink from, final StaffSink to) {
    to.setStaffIdentifier(from.getStaffIdentifier());
    to.setStaffTitleTypeCode(from.getStaffTitleTypeCode());
    to.setStaffGivenName(from.getStaffGivenName());
    to.setStaffMiddleName(from.getStaffMiddleName());
    to.setStaffFamilyName(from.getStaffFamilyName());
    to.setStaffDoEUserIdentifier(from.getStaffDoEUserIdentifier());
    to.setStaffDoEEmailAddress(from.getStaffDoEEmailAddress());
    to.setStaffStreetAddressLine1(from.getStaffStreetAddressLine1());
    to.setStaffStreetAddressSuburbName(from.getStaffStreetAddressSuburbName());
    to.setStaffStreetAddressPostalCodeIdentifier(from.getStaffStreetAddressPostalCodeIdentifier());
    to.setStaffStreetAddressStateCode(from.getStaffStreetAddressStateCode());
    to.setStaffPhoneNumber(from.getStaffPhoneNumber());
    to.setStaffGenderTypeCode(from.getStaffGenderTypeCode());
    to.setStaffWorkingWithChildrenCheckNumber(from.getStaffWorkingWithChildrenCheckNumber());
    to.setStaffSAPEmployeeIdentifier(from.getStaffSAPEmployeeIdentifier());
    to.setStaffSAPSystemIdentifier(from.getStaffSAPSystemIdentifier());
    to.setStaffStatus(from.getStaffStatus());
    to.setStaffMobileNumber(from.getStaffMobileNumber());
    to.setMetacreatedby(from.getMetacreatedby());
    to.setMetacreateddate(from.getMetacreateddate());
    to.setMetaprovidedby(from.getMetaprovidedby());
  }

  private ValueMapper<StaffSource, StaffSink> convertStaffSourceToStaffSink() {
    return staffSource -> {
      List<Employment> employmentList = new ArrayList<>();

      // The key fields are used to sync changes with staff purge
      boolean hasKeyField =
          StringUtils.isNotBlank(staffSource.getStaffschoolCode())
              || StringUtils.isNotBlank(staffSource.getSchoolStaffIdentifier())
              || StringUtils.isNotBlank(staffSource.getStaffFunctionalUnitCode());

      // APISTAFF-618 Map employment record if:
      //  - has school code
      //  - has staff position (with any of the key fields defined)
      if (StringUtils.isNotBlank(staffSource.getStaffschoolCode())
          || (StringUtils.isNotBlank(staffSource.getStaffPositionName()) && hasKeyField)) {
        Employment employment =
            Employment.newBuilder()
                .setStaffSchoolCode(staffSource.getStaffschoolCode())
                .setStaffPositionName(staffSource.getStaffPositionName())
                .setStaffFunctionalUnitCode(staffSource.getStaffFunctionalUnitCode())
                .setStaffFunctionalUnitName(staffSource.getStaffFunctionalUnitName())
                .setStaffManagementUnitCode(staffSource.getStaffManagementUnitCode())
                .setStaffManagementUnitName(staffSource.getStaffManagementUnitName())
                .setStaffPayScaleSubGroup(staffSource.getStaffPayScaleSubGroup())
                .setSchoolStaffIdentifier(staffSource.getSchoolStaffIdentifier())
                .build();
        employmentList.add(employment);
      }

      return StaffSink.newBuilder()
          .setStaffIdentifier(staffSource.getStaffIdentifier())
          .setStaffTitleTypeCode(staffSource.getStaffTitleTypeCode())
          .setStaffGivenName(staffSource.getStaffGivenName())
          .setStaffMiddleName(staffSource.getStaffMiddleName())
          .setStaffFamilyName(staffSource.getStaffFamilyName())
          .setStaffDoEUserIdentifier(staffSource.getStaffDoEUserIdentifier())
          .setStaffDoEEmailAddress(staffSource.getStaffDoEEmailAddress())
          .setStaffStreetAddressLine1(staffSource.getStaffStreetAddressLine1())
          .setStaffStreetAddressSuburbName(staffSource.getStaffStreetAddressSuburbName())
          .setStaffStreetAddressPostalCodeIdentifier(
              staffSource.getStaffStreetAddressPostalCodeIdentifier())
          .setStaffStreetAddressStateCode(staffSource.getStaffStreetAddressStateCode())
          .setStaffPhoneNumber(staffSource.getStaffPhoneNumber())
          .setStaffGenderTypeCode(staffSource.getStaffGenderTypeCode())
          .setStaffWorkingWithChildrenCheckNumber(
              staffSource.getStaffWorkingWithChildrenCheckNumber())
          .setStaffSAPEmployeeIdentifier(staffSource.getStaffSAPEmployeeIdentifier())
          .setStaffSAPSystemIdentifier(staffSource.getStaffSAPSystemIdentifier())
          .setStaffStatus(staffSource.getStaffStatus())
          .setStaffMobileNumber(staffSource.getStaffMobileNumber())
          .setStaffEmployment(employmentList)
          .setMetacreatedby(staffSource.getMetacreatedby())
          .setMetacreateddate(staffSource.getMetacreateddate())
          .setMetaprovidedby(staffSource.getMetaprovidedby())
          .build();
    };
  }
}
