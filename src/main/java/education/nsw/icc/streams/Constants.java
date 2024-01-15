package education.nsw.icc.streams;

public class Constants {
  public static final String DELETE_INDICATOR = "DELETE";
  public static final String NULL_INDICATOR = "null";
  public static final String ARCHIVED_STATUS = "1";

  public enum RecordType {
    DELETE_EMPLOYEE,
    DELETE_EMPLOYMENT,
    ACTIVE
  }
}
