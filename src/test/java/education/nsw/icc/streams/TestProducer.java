package education.nsw.icc.streams;

import education.nsw.streams.avro.api.StaffSource;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TestProducer {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Properties props = new Properties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "AvroProducerTest");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "dl0991kfkab0001.nsw.education:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://dl0991kfkar0001.nsw.education:8081");
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        StaffSource staffSource = createStaffSource();
        //StaffSource staffSource = getStaffListWhichVaryByFieldsWhichAreNotInOutputTopic3();
        //StaffSource staffSource =  createStaffSourceWithSchoolNullValues();
        KafkaProducer<String, StaffSource> producer = new KafkaProducer<>(props);
        Future<RecordMetadata> metadata = producer.send(new ProducerRecord<String, StaffSource>("staff-hub-db-2", "{\"staffIdentifier\":\""+staffSource.getStaffIdentifier()+"\"}", staffSource));
        while(!metadata.isDone());
        RecordMetadata result = metadata.get();
        System.out.println(result);

        producer.flush();
        producer.close();
    }

    private static StaffSource createStaffSource() {
        StaffSource staffSource = new StaffSource();
        staffSource.setStaffIdentifier("100001-1");
        staffSource.setStaffTitleTypeCode("Mrs");
        staffSource.setStaffGivenName("Alison");
        staffSource.setStaffMiddleName("joy");
        staffSource.setStaffFamilyName("Benton");
        staffSource.setStaffDoEUserIdentifier("alison.joy");
        staffSource.setStaffDoEEmailAddress("alison.joy@google.com");
        staffSource.setStaffStreetAddressLine1("oxley cres");
        staffSource.setStaffStreetAddressSuburbName("port mac");
        staffSource.setStaffStreetAddressStateCode("NSW");
        staffSource.setStaffStreetAddressPostalCodeIdentifier("2145");
        staffSource.setStaffPhoneNumber("12345334");
        staffSource.setStaffGenderTypeCode("F");
        staffSource.setStaffWorkingWithChildrenCheckNumber("23423");
        staffSource.setStaffschoolCode("8184");
        staffSource.setSchoolStaffIdentifier("3444");
        staffSource.setStaffPositionName("teacher");
        staffSource.setEmploymentStatus("fulltime");
        staffSource.setHoursPerWeek("40");
        staffSource.setPositionType("NT");
        staffSource.setMetacreatedby("admin");
        staffSource.setMetacreateddate(Instant.now());
        staffSource.setMetaprovidedby("admin");
        return staffSource;
    }

    private static StaffSource getStaffListWhichVaryByFieldsWhichAreNotInOutputTopic1() {

        StaffSource staffSource1 = new StaffSource();
        staffSource1.setStaffIdentifier("9003");
        staffSource1.setStaffTitleTypeCode("Mr");
        staffSource1.setStaffGivenName("Joe");
        staffSource1.setStaffMiddleName("joy");
        staffSource1.setStaffFamilyName("Benton");
        staffSource1.setStaffDoEUserIdentifier("joe.joy");
        staffSource1.setStaffDoEEmailAddress("joe.joy@google.com");
        staffSource1.setStaffStreetAddressLine1("oxley cres");
        staffSource1.setStaffStreetAddressSuburbName("port mac");
        staffSource1.setStaffStreetAddressStateCode("NSW");
        staffSource1.setStaffStreetAddressPostalCodeIdentifier("2145");
        staffSource1.setStaffPhoneNumber("12345334");
        staffSource1.setStaffGenderTypeCode("F");
        staffSource1.setStaffWorkingWithChildrenCheckNumber("23423");
        staffSource1.setStaffschoolCode("3972");
        staffSource1.setSchoolStaffIdentifier("1755367");
        staffSource1.setStaffPositionName("school admin officer");
        staffSource1.setEmploymentStatus("short term temp");
        staffSource1.setHoursPerWeek("0");
        staffSource1.setPositionType("NT");
        staffSource1.setMetacreatedby("admin");
        staffSource1.setMetacreateddate(Instant.now());
        staffSource1.setMetaprovidedby("admin");

        return staffSource1;
    }

    private static StaffSource getStaffListWhichVaryByFieldsWhichAreNotInOutputTopic2() {
        StaffSource staffSource2 = new StaffSource();
        staffSource2.setStaffIdentifier("9003");
        staffSource2.setStaffTitleTypeCode("Mr");
        staffSource2.setStaffGivenName("Joe");
        staffSource2.setStaffMiddleName("joy");
        staffSource2.setStaffFamilyName("Benton");
        staffSource2.setStaffDoEUserIdentifier("joe.joy");
        staffSource2.setStaffDoEEmailAddress("joe.joy@google.com");
        staffSource2.setStaffStreetAddressLine1("oxley cres");
        staffSource2.setStaffStreetAddressSuburbName("port mac");
        staffSource2.setStaffStreetAddressStateCode("NSW");
        staffSource2.setStaffStreetAddressPostalCodeIdentifier("2145");
        staffSource2.setStaffPhoneNumber("12345334");
        staffSource2.setStaffGenderTypeCode("F");
        staffSource2.setStaffWorkingWithChildrenCheckNumber("23423");
        staffSource2.setStaffschoolCode("4221");
        staffSource2.setSchoolStaffIdentifier("1755367");
        staffSource2.setStaffPositionName("school admin officer");
        staffSource2.setEmploymentStatus("short term temp");
        staffSource2.setHoursPerWeek("0");
        staffSource2.setPositionType("NT");
        staffSource2.setMetacreatedby("admin");
        staffSource2.setMetacreateddate(Instant.now());
        staffSource2.setMetaprovidedby("admin");
        return staffSource2;
    }

    private static StaffSource getStaffListWhichVaryByFieldsWhichAreNotInOutputTopic3() {
        StaffSource staffSource3 = new StaffSource();
        staffSource3.setStaffIdentifier("9004-4");
        staffSource3.setStaffTitleTypeCode("Mr");
        staffSource3.setStaffGivenName("Joe");
        staffSource3.setStaffMiddleName("joy");
        staffSource3.setStaffFamilyName("Benton");
        staffSource3.setStaffDoEUserIdentifier("joe.joy");
        staffSource3.setStaffDoEEmailAddress("joe.joy@google.com");
        staffSource3.setStaffStreetAddressLine1("oxley cres");
        staffSource3.setStaffStreetAddressSuburbName("port mac");
        staffSource3.setStaffStreetAddressStateCode("NSW");
        staffSource3.setStaffStreetAddressPostalCodeIdentifier("2145");
        staffSource3.setStaffPhoneNumber("12345334");
        staffSource3.setStaffGenderTypeCode("F");
        staffSource3.setStaffWorkingWithChildrenCheckNumber("23423");
        staffSource3.setStaffschoolCode("4221");
        staffSource3.setSchoolStaffIdentifier("3986881");
        staffSource3.setStaffPositionName("school admin officer");
        staffSource3.setEmploymentStatus("Long term temp");
        staffSource3.setHoursPerWeek("31.25");
        staffSource3.setPositionType("NT");
        staffSource3.setMetacreatedby("admin");
        staffSource3.setMetacreateddate(Instant.now());
        staffSource3.setMetaprovidedby("admin");
        return staffSource3;
    }

    private static StaffSource createStaffSourceWithSchoolNullValues() {
        StaffSource staffSource = new StaffSource();
        staffSource.setStaffIdentifier("9004-52");
        staffSource.setStaffTitleTypeCode("Mrs");
        staffSource.setStaffGivenName("Alison1");
        staffSource.setStaffMiddleName("joy");
        staffSource.setStaffFamilyName("Benton");
        staffSource.setStaffDoEUserIdentifier("alison.joy");
        staffSource.setStaffDoEEmailAddress("alison.joy@google.com");
        staffSource.setStaffStreetAddressLine1("oxley cres");
        staffSource.setStaffStreetAddressSuburbName("port mac");
        staffSource.setStaffStreetAddressStateCode("NSW");
        staffSource.setStaffStreetAddressPostalCodeIdentifier("2145");
        staffSource.setStaffPhoneNumber("12345334");
        staffSource.setStaffGenderTypeCode("F");
        staffSource.setStaffWorkingWithChildrenCheckNumber("23423");
        staffSource.setStaffschoolCode(null);
        staffSource.setSchoolStaffIdentifier(null);
        staffSource.setStaffPositionName(null);
        staffSource.setEmploymentStatus(null);
        staffSource.setHoursPerWeek(null);
        staffSource.setPositionType(null);
        staffSource.setMetacreatedby("admin");
        staffSource.setMetacreateddate(Instant.now());
        staffSource.setMetaprovidedby("admin");
        return staffSource;
    }

}
