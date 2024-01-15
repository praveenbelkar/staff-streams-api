package education.nsw.icc.streams;

import static education.nsw.icc.streams.Constants.DELETE_INDICATOR;
import static education.nsw.icc.streams.Constants.NULL_INDICATOR;

import education.nsw.icc.streams.Constants.RecordType;
import education.nsw.streams.avro.api.Employment;
import education.nsw.streams.avro.api.StaffSink;
import education.nsw.streams.avro.api.StaffSource;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class StaffUtil {

  public static RecordType recordType(StaffSource value) {
    if (Objects.equals(value.getStaffIdentifier(), DELETE_INDICATOR)) {
      return (StringUtils.isAllBlank(
              value.getStaffschoolCode(),
              value.getSchoolStaffIdentifier(),
              value.getStaffFunctionalUnitCode()))
          ? RecordType.DELETE_EMPLOYEE
          : RecordType.DELETE_EMPLOYMENT;
    } else {
      return RecordType.ACTIVE;
    }
  }

  public static RecordType recordType(StaffSink value) {
    if (Objects.equals(value.getStaffIdentifier(), DELETE_INDICATOR)) {
      return CollectionUtils.isEmpty(value.getStaffEmployment())
          ? RecordType.DELETE_EMPLOYEE
          : RecordType.DELETE_EMPLOYMENT;
    } else {
      return RecordType.ACTIVE;
    }
  }

  public static String makeStaffEmploymentKey(String key, StaffSource staffSource) {
    String part1 =
        StringUtils.isBlank(staffSource.getStaffschoolCode())
            ? NULL_INDICATOR
            : staffSource.getStaffschoolCode();
    String part2 =
        StringUtils.isBlank(staffSource.getSchoolStaffIdentifier())
            ? NULL_INDICATOR
            : staffSource.getSchoolStaffIdentifier();
    String part3 =
        StringUtils.isBlank(staffSource.getStaffFunctionalUnitCode())
            ? NULL_INDICATOR
            : staffSource.getStaffFunctionalUnitCode();
    return key + ":" + part1 + ":" + part2 + ":" + part3;
  }

  public static String makeStaffEmploymentKey(Employment employment) {
    String part1 =
        StringUtils.isBlank(employment.getStaffSchoolCode())
            ? NULL_INDICATOR
            : employment.getStaffSchoolCode();
    String part2 =
        StringUtils.isBlank(employment.getSchoolStaffIdentifier())
            ? NULL_INDICATOR
            : employment.getSchoolStaffIdentifier();
    String part3 =
        StringUtils.isBlank(employment.getStaffFunctionalUnitCode())
            ? NULL_INDICATOR
            : employment.getStaffFunctionalUnitCode();
    return part1 + ":" + part2 + ":" + part3;
  }

  public static boolean isSame(Employment employment, Employment employmentOfValue) {
    int comparePart1 =
        StringUtils.compare(
            employment.getStaffSchoolCode(), employmentOfValue.getStaffSchoolCode());
    int comparePart2 =
        StringUtils.compare(
            employment.getSchoolStaffIdentifier(), employmentOfValue.getSchoolStaffIdentifier());
    int comparePart3 =
        StringUtils.compare(
            employment.getStaffFunctionalUnitCode(),
            employmentOfValue.getStaffFunctionalUnitCode());
    return comparePart1 == 0 && comparePart2 == 0 && comparePart3 == 0;
  }
}
