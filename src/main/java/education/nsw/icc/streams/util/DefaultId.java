package education.nsw.icc.streams.util;

import java.util.Objects;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

public class DefaultId {
  private String staffIdentifier;

  public DefaultId() {}

  @JsonbCreator
  public DefaultId(@JsonbProperty("staffIdentifier") String staffIdentifier) {
    this.staffIdentifier = staffIdentifier;
  }

  public String getStaffIdentifier() {
    return staffIdentifier;
  }

  public void setStaffIdentifier(String staffIdentifier) {
    this.staffIdentifier = staffIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DefaultId defaultId = (DefaultId) o;
    return Objects.equals(staffIdentifier, defaultId.staffIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(staffIdentifier);
  }

  @Override
  public String toString() {
    return "DefaultId{" + "staffIdentifier='" + staffIdentifier + '\'' + '}';
  }
}
