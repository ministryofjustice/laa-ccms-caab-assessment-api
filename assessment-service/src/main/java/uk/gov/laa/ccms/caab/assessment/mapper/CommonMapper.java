package uk.gov.laa.ccms.caab.assessment.mapper;

import java.util.Base64;
import org.mapstruct.Mapper;

/**
 * Common mappings which can be used by other mappers.
 */
@Mapper(componentModel = "spring")
public interface CommonMapper {

  /**
   * Global mapper to default Booleans to FALSE in the case of a null source value.
   *
   * @param flag - the source value
   * @return the source value, or FALSE.
   */
  default Boolean toBoolean(Boolean flag) {
    return flag != null ? flag : Boolean.FALSE;
  }


  default String toBase64String(byte[] bytes) {
    return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
  }

  default byte[] toByteArrayFromBase64EncodedString(String base64EncodedString) {
    return Base64.getDecoder().decode(base64EncodedString);
  }
}

