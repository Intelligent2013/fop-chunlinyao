/*
 * $Id$
 * Copyright (C) 2001 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 *
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @version $Rev$ $Name$
 */

package org.apache.fop.fo;

/**
 * Data class containing the Flow Object names and associated integer
 * constants.
 */

public class FObjectNames {

    private static final String tag = "$Name$";
    private static final String revision = "$Revision$";

    /**
     * Constant for matching Flow Object defined in <i>XSLFO</i>.
     */
    public static final int
                                  NO_FO = 0,
                             BASIC_LINK = 1,
                          BIDI_OVERRIDE = 2,
                                  BLOCK = 3,
                        BLOCK_CONTAINER = 4,
                              CHARACTER = 5,
                          COLOR_PROFILE = 6,
      CONDITIONAL_PAGE_MASTER_REFERENCE = 7,
                           DECLARATIONS = 8,
                       EXTERNAL_GRAPHIC = 9,
                                  FLOAT = 10,
                                   FLOW = 11,
                               FOOTNOTE = 12,
                          FOOTNOTE_BODY = 13,
                   INITIAL_PROPERTY_SET = 14,
                                 INLINE = 15,
                       INLINE_CONTAINER = 16,
                INSTREAM_FOREIGN_OBJECT = 17,
                      LAYOUT_MASTER_SET = 18,
                                 LEADER = 19,
                             LIST_BLOCK = 20,
                              LIST_ITEM = 21,
                         LIST_ITEM_BODY = 22,
                        LIST_ITEM_LABEL = 23,
                                 MARKER = 24,
                             MULTI_CASE = 25,
                       MULTI_PROPERTIES = 26,
                     MULTI_PROPERTY_SET = 27,
                           MULTI_SWITCH = 28,
                           MULTI_TOGGLE = 29,
                            PAGE_NUMBER = 30,
                   PAGE_NUMBER_CITATION = 31,
                          PAGE_SEQUENCE = 32,
                   PAGE_SEQUENCE_MASTER = 33,
                           REGION_AFTER = 34,
                          REGION_BEFORE = 35,
                            REGION_BODY = 36,
                             REGION_END = 37,
                           REGION_START = 38,
    REPEATABLE_PAGE_MASTER_ALTERNATIVES = 39,
       REPEATABLE_PAGE_MASTER_REFERENCE = 40,
                        RETRIEVE_MARKER = 41,
                                   ROOT = 42,
                     SIMPLE_PAGE_MASTER = 43,
           SINGLE_PAGE_MASTER_REFERENCE = 44,
                         STATIC_CONTENT = 45,
                                  TABLE = 46,
                      TABLE_AND_CAPTION = 47,
                             TABLE_BODY = 48,
                          TABLE_CAPTION = 49,
                             TABLE_CELL = 50,
                           TABLE_COLUMN = 51,
                           TABLE_FOOTER = 52,
                           TABLE_HEADER = 53,
                              TABLE_ROW = 54,
                                  TITLE = 55,
                                WRAPPER = 56,

                                LAST_FO = WRAPPER;

    /**
     * Array containing the local names of all of the elements in the
     * <i>FO</i> namespace.  The array is effectively 1-based as the zero
     * index does not correspond to any FO element.  The list of
     * <tt>int</tt> constants must be kept in sync with this array, as the
     * constants are used to index into the array.
     */
    public static final String[] foLocalNames = {
        "no-fo",
        "basic-link",
        "bidi-override",
        "block",
        "block-container",
        "character",
        "color-profile",
        "conditional-page-master-reference",
        "declarations",
        "external-graphic",
        "float",
        "flow",
        "footnote",
        "footnote-body",
        "initial-property-set",
        "inline",
        "inline-container",
        "instream-foreign-object",
        "layout-master-set",
        "leader",
        "list-block",
        "list-item",
        "list-item-body",
        "list-item-label",
        "marker",
        "multi-case",
        "multi-properties",
        "multi-property-set",
        "multi-switch",
        "multi-toggle",
        "page-number",
        "page-number-citation",
        "page-sequence",
        "page-sequence-master",
        "region-after",
        "region-before",
        "region-body",
        "region-end",
        "region-start",
        "repeatable-page-master-alternatives",
        "repeatable-page-master-reference",
        "retrieve-marker",
        "root",
        "simple-page-master",
        "single-page-master-reference",
        "static-content",
        "table",
        "table-and-caption",
        "table-body",
        "table-caption",
        "table-cell",
        "table-column",
        "table-footer",
        "table-header",
        "table-row",
        "title",
        "wrapper"
    };
}
