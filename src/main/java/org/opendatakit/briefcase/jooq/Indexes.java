/*
 * This file is generated by jOOQ.
 */
package org.opendatakit.briefcase.jooq;


import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.Internal;
import org.opendatakit.briefcase.jooq.tables.FormMetadata;


/**
 * A class modelling indexes of tables of the <code>PUBLIC</code> schema.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Indexes {

  // -------------------------------------------------------------------------
  // INDEX definitions
  // -------------------------------------------------------------------------

  public static final Index SYS_IDX_SYS_CT_10133_10137 = Indexes0.SYS_IDX_SYS_CT_10133_10137;
  public static final Index SYS_IDX_SYS_PK_10131_10134 = Indexes0.SYS_IDX_SYS_PK_10131_10134;

  // -------------------------------------------------------------------------
  // [#1459] distribute members to avoid static initialisers > 64kb
  // -------------------------------------------------------------------------

  private static class Indexes0 {
    public static Index SYS_IDX_SYS_CT_10133_10137 = Internal.createIndex("SYS_IDX_SYS_CT_10133_10137", FormMetadata.FORM_METADATA, new OrderField[]{FormMetadata.FORM_METADATA.FORM_NAME, FormMetadata.FORM_METADATA.FORM_ID, FormMetadata.FORM_METADATA.FORM_VERSION}, true);
    public static Index SYS_IDX_SYS_PK_10131_10134 = Internal.createIndex("SYS_IDX_SYS_PK_10131_10134", FormMetadata.FORM_METADATA, new OrderField[]{FormMetadata.FORM_METADATA.ID}, true);
  }
}