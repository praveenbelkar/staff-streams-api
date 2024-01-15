package education.nsw.icc.streams;

import education.nsw.icc.streams.util.DefaultId;
import education.nsw.streams.avro.api.StaffSource;
import java.time.Instant;
import java.util.Objects;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaffTransformer
    implements Transformer<DefaultId, StaffSource, KeyValue<DefaultId, StaffSource>> {

  private static final Logger LOG = LoggerFactory.getLogger(StaffTransformer.class);
  private static final StaffSource nullStaffSource = new StaffSource();
  private final String storeName;
  private KeyValueStore<String, StaffSource> staffStore;

  public StaffTransformer(String storeName) {
    this.storeName = storeName;
  }

  @Override
  public void init(ProcessorContext context) {
    this.staffStore = (KeyValueStore<String, StaffSource>) context.getStateStore(storeName);
  }

  @Override
  public KeyValue<DefaultId, StaffSource> transform(final DefaultId key, final StaffSource value) {
    if (key == null || value == null) {
      return null;
    }
    LOG.debug("transform**********");
    LOG.debug("key: " + key);

    final boolean result = isDuplicate(key.getStaffIdentifier(), value);
    LOG.debug("isDuplicate: " + result);
    return result ? null : new KeyValue<>(key, value);
  }

  @Override
  public void close() {}

  private boolean isDuplicate(String key, StaffSource value) {
    final String uniqueId = StaffUtil.makeStaffEmploymentKey(key, value);

    switch (StaffUtil.recordType(value)) {
      case DELETE_EMPLOYEE:
        // remove all keys for the employee
        try (KeyValueIterator<String, StaffSource> it = staffStore.range(key + ":", key + ":~")) {
          while (it.hasNext()) {
            staffStore.delete(it.next().key);
          }
        }
        return false;
      case DELETE_EMPLOYMENT:
        // remove key for this employment
        staffStore.delete(uniqueId);
        return false;
      case ACTIVE:
      default:
    }

    boolean exists = false;
    final StaffSource existingValue = staffStore.get(uniqueId);
    if (existingValue != null) {
      final String saveMetacreatedby = value.getMetacreatedby();
      final Instant saveMetacreateddate = value.getMetacreateddate();
      final String saveMetaprovidedby = value.getMetaprovidedby();

      value.setMetacreatedby(existingValue.getMetacreatedby());
      value.setMetacreateddate(existingValue.getMetacreateddate());
      value.setMetaprovidedby(existingValue.getMetaprovidedby());
      exists = Objects.equals(value, existingValue);

      // restore values omitted for equals check
      value.setMetacreatedby(saveMetacreatedby);
      value.setMetacreateddate(saveMetacreateddate);
      value.setMetaprovidedby(saveMetaprovidedby);
    }

    // add or replace
    staffStore.put(uniqueId, value);

    // A null key means no employment
    // Remove the null key when adding a valid employment
    final String nullKey = StaffUtil.makeStaffEmploymentKey(key, nullStaffSource);
    if (!Objects.equals(uniqueId, nullKey)) {
      if (staffStore.get(nullKey) != null) staffStore.delete(nullKey);
    }

    return exists;
  }
}
