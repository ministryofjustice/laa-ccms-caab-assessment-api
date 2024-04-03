package uk.gov.laa.ccms.caab.assessment.mapper;

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
}

