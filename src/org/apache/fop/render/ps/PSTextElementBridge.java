/*
 * $Id$
 * Copyright (C) 2001-2003 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.render.ps;

//import org.apache.batik.gvt.TextNode;
import org.apache.batik.bridge.SVGTextElementBridge;
import org.apache.batik.bridge.BridgeContext;
//import org.apache.batik.bridge.TextUtilities;
import org.apache.batik.gvt.GraphicsNode;

import org.apache.fop.layout.FontInfo;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bridge class for the &lt;text> element.
 * This bridge will use the direct text painter if the text
 * for the element is simple.
 *
 * @author <a href="mailto:fop-dev@xml.apache.org">Apache XML FOP Development Team</a>
 * @version $Id$
 */
public class PSTextElementBridge extends SVGTextElementBridge {
    
    //private PSTextPainter textPainter;

    /**
     * Constructs a new bridge for the &lt;text> element.
     * @param fi the font infomration
     */
    public PSTextElementBridge(FontInfo fi) {
        //textPainter = new PSTextPainter(fi);
    }

    /**
     * Create a text element bridge.
     * This set the text painter on the node if the text is simple.
     * @param ctx the bridge context
     * @param e the svg element
     * @return the text graphics node created by the super class
     */
    public GraphicsNode createGraphicsNode(BridgeContext ctx, Element e) {
        GraphicsNode node = super.createGraphicsNode(ctx, e);
        /*
        if (node != null && isSimple(ctx, e, node)) {
            ((TextNode)node).setTextPainter(getTextPainter());
        }*/
        return node;
    }

    /*
    private PSTextPainter getTextPainter() {
        return textPainter;
    }
    */

    /**
     * Check if text element contains simple text.
     * This checks the children of the text element to determine
     * if the text is simple. The text is simple if it can be rendered
     * with basic text drawing algorithms. This means there are no
     * alternate characters, the font is known and there are no effects
     * applied to the text.
     *
     * @param ctx the bridge context
     * @param element the svg text element
     * @param node the graphics node
     * @return true if this text is simple of false if it cannot be
     *         easily rendered using normal drawString on the PDFGraphics2D
     */
    private boolean isSimple(BridgeContext ctx, Element element, GraphicsNode node) {
        /*
        // Font size, in user space units.
        float fs = TextUtilities.convertFontSize(element).floatValue();
        // PDF cannot display fonts over 36pt
        if (fs > 36) {
            return false;
        }
        */

        
        for (Node n = element.getFirstChild();
                n != null;
                n = n.getNextSibling()) {

            switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:

                if (n.getLocalName().equals(SVG_TSPAN_TAG)
                    || n.getLocalName().equals(SVG_ALT_GLYPH_TAG)) {
                    return false;
                } else if (n.getLocalName().equals(SVG_TEXT_PATH_TAG)) {
                    return false;
                } else if (n.getLocalName().equals(SVG_TREF_TAG)) {
                    return false;
                }
                break;
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
            }
        }

        /*if (CSSUtilities.convertFilter(element, node, ctx) != null) {
            return false;
        }*/

        return true;
    }
}

