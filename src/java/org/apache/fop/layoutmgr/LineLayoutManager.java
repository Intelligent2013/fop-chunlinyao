/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.layoutmgr;

import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.PropertyManager;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.CommonHyphenation;
import org.apache.fop.layout.hyphenation.Hyphenation;
import org.apache.fop.layout.hyphenation.Hyphenator;
import org.apache.fop.traits.BlockProps;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.Resolveable;

import java.util.ListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import org.apache.fop.traits.MinOptMax;

/**
 * LayoutManager for lines. It builds one or more lines containing
 * inline areas generated by its sub layout managers.
 * A break is found for each line which may contain one of more
 * breaks from the child layout managers.
 * Once a break is found then it is return for the parent layout
 * manager to handle.
 * When the areas are being added to the page this manager
 * creates a line area to contain the inline areas added by the
 * child layout managers.
 */
public class LineLayoutManager extends InlineStackingLayoutManager {

    /**
     * Private class to store information about inline breaks.
     * Each value holds the start and end indexes into a List of
     * inline break positions.
     */
    private static class LineBreakPosition extends LeafPosition {
        // int iPos;
        double dAdjust; // Percentage to adjust (stretch or shrink)
        double ipdAdjust; // Percentage to adjust (stretch or shrink)
        int startIndent;
        int lineHeight;
        int baseline;

        LineBreakPosition(LayoutManager lm, int iBreakIndex,
                          double ipdA, double adjust, int ind, int lh, int bl) {
            super(lm, iBreakIndex);
            // iPos = iBreakIndex;
            ipdAdjust = ipdA;
            dAdjust = adjust;
            startIndent = ind;
            lineHeight = lh;
            baseline = bl;
        }
    }


    /** Break positions returned by inline content. */
    private List vecInlineBreaks = new ArrayList();

    private BreakPoss prevBP = null; // Last confirmed break position
    private int bTextAlignment = TextAlign.JUSTIFY;
    private Length textIndent;
    private int iIndents = 0;
    private CommonHyphenation hyphProps;

    private int lineHeight;
    private int lead;
    private int follow;

    // inline start pos when adding areas
    private int iStartPos = 0;

    /**
     * Create a new Line Layout Manager.
     * This is used by the block layout manager to create
     * line managers for handling inline areas flowing into line areas.
     *
     * @param lh the default line height
     * @param l the default lead, from top to baseline
     * @param f the default follow, from baseline to bottom
     */
    public LineLayoutManager(int lh, int l, int f) {
        lineHeight = lh;
        lead = l;
        follow = f;
        initialize(); // Normally done when started by parent!
    }

    /**
     * Initialize the properties for this layout manager.
     * The properties are from the block area.
     * @see org.apache.fop.layoutmgr.AbstractLayoutManager#initProperties(PropertyManager)
     */
    protected void initProperties(PropertyManager propMgr) {
        CommonMarginBlock marginProps = propMgr.getMarginProps();
        iIndents = marginProps.startIndent + marginProps.endIndent;
        BlockProps blockProps = propMgr.getBlockProps();
        bTextAlignment = blockProps.textAlign;
        textIndent = blockProps.firstIndent;
        hyphProps = propMgr.getHyphenationProps();
    }

    /**
     * Call child layout managers to generate content.
     * This gets the next break which is a full line.
     *
     * @param context the layout context for finding breaks
     * @return the next break position
     */
    public BreakPoss getNextBreakPoss(LayoutContext context) {
        // Get a break from currently active child LM
        // Set up constraints for inline level managers

        LayoutManager curLM ; // currently active LM
        BreakPoss prev = null;
        BreakPoss bp = null; // proposed BreakPoss

        ArrayList vecPossEnd = new ArrayList();

        // IPD remaining in line
        MinOptMax availIPD = context.getStackLimit();

        // QUESTION: maybe LayoutContext holds the Properties which
        // come from block-level?

        LayoutContext inlineLC = new LayoutContext(context);

        clearPrevIPD();
        int iPrevLineEnd = vecInlineBreaks.size();

        if (iPrevLineEnd == 0 && bTextAlignment == TextAlign.START) {
            availIPD.subtract(new MinOptMax(textIndent.getValue()));
        }
        prevBP = null;

        while ((curLM = getChildLM()) != null) {
            // INITIALIZE LAYOUT CONTEXT FOR CALL TO CHILD LM
            // First break for the child LM in each of its areas
            boolean bFirstBPforLM = (vecInlineBreaks.isEmpty()
                    || (((BreakPoss) vecInlineBreaks.get(vecInlineBreaks.size() - 1)).
                                      getLayoutManager() != curLM));

            // Need previous breakpoint! ATTENTION when backing up for hyphenation!
            prev = (vecInlineBreaks.isEmpty())
                    ? null
                    : (BreakPoss) vecInlineBreaks.get(vecInlineBreaks.size() - 1);
            initChildLC(inlineLC, prev,
                        (vecInlineBreaks.size() == iPrevLineEnd),
                        bFirstBPforLM, new SpaceSpecifier(true));


            /* If first BP in this line but line is not first in this
             * LM and previous line end decision was not forced (LINEFEED),
             * then set the SUPPRESS_LEADING_SPACE flag.
             */
            inlineLC.setFlags(LayoutContext.SUPPRESS_LEADING_SPACE,
                              (vecInlineBreaks.size() == iPrevLineEnd
                               && !vecInlineBreaks.isEmpty()
                               && ((BreakPoss) vecInlineBreaks.get(vecInlineBreaks.size() - 1)).
                                    isForcedBreak() == false));

            // GET NEXT POSSIBLE BREAK FROM CHILD LM
            // prevBP = bp;
            if ((bp = curLM.getNextBreakPoss(inlineLC)) != null) {
                // Add any space before and previous content dimension
                MinOptMax prevIPD = updatePrevIPD(bp, prev,
                                                  (vecInlineBreaks.size() == iPrevLineEnd),
                                                  inlineLC.isFirstArea());
                MinOptMax bpDim =
                  MinOptMax.add(bp.getStackingSize(), prevIPD);

                // check if this bp fits in line
                boolean bBreakOK = couldEndLine(bp);
                if (bBreakOK) {
                    /* Add any non-conditional trailing space, assuming we
                     * end the line here. If we can't break here, we just
                     * check if the content fits.
                     */
                    bpDim.add(bp.resolveTrailingSpace(true));
                }
                // TODO: stop if linebreak is forced (NEWLINE)
                // PROBLEM: interaction with wrap which can be set
                // at lower levels!
                // System.err.println("BPdim=" + bpDim.opt);

                // Check if proposed area would fit in line
                if (bpDim.min > availIPD.max) {
                    // See if we have already found a potential break
                    //if (vecPossEnd.size() > 0) break;

                    // This break position doesn't fit
                    // TODO: If we are in nowrap, we use it as is!
                    if (bTextAlignment == TextAlign.JUSTIFY || prevBP == null) {
                        // If we are already in a hyphenation loop, then stop.

                        if (inlineLC.tryHyphenate()) {
                            if (prevBP == null) {
                                vecInlineBreaks.add(bp);
                                prevBP = bp;
                            }
                            break;
                        }
                        // Otherwise, prepare to try hyphenation
                        if (!bBreakOK) {
                            // Make sure we collect the entire word!
                            vecInlineBreaks.add(bp);
                            continue;
                        }

                        inlineLC.setHyphContext(
                          getHyphenContext((prevBP == null) ? prev : prevBP, bp));
                        if (inlineLC.getHyphContext() == null) {
                            if (prevBP == null) {
                                vecInlineBreaks.add(bp);
                                prevBP = bp;
                            }
                            break;
                        }
                        inlineLC.setFlags(LayoutContext.TRY_HYPHENATE,
                                          true);
                        // Reset to previous acceptable break
                        resetBP((prevBP == null) ? prev : prevBP);
                    } else {
                        /* If we are not in justified text, we can end the line at
                         * prevBP.
                         */
                        if (prevBP == null) {
                            vecInlineBreaks.add(bp);
                            prevBP = bp;
                        }
                        break;
                    }
                } else {
                    // Add the BP to the list whether or not we can break
                    vecInlineBreaks.add(bp);
                    // Handle end of this LM's areas
                    if (bBreakOK) {
                        prevBP = bp; // Save reference to this BP
                        if (bp.isForcedBreak()) {
                            break;
                        }
                        if (bpDim.max >= availIPD.min) {
                            /* This is a possible line BP (line could be filled)
                             * bpDim.max >= availIPD.min
                             * Keep this as a possible break, depending on
                             * "cost". We will choose lowest cost.
                             * Cost depends on stretch
                             * (ie, bpDim.opt closes to availIPD.opt), keeps
                             * and hyphenation.
                             */
                            vecPossEnd.add(new BreakCost(bp,
                                    Math.abs(availIPD.opt - bpDim.opt)));
                        }
                        // Otherwise it's short
                    } else {
                        /* Can't end line here. */
                    }
                } // end of bpDim.min <= availIPD.max
            // end of getNextBreakPoss!=null on current child LM
            } else {
                /* The child LM can return a null BreakPoss if it has
                 * nothing (more) to layout. This can happen when backing
                 * up. Just try the next child LM.
                 */
            }
            if (inlineLC.tryHyphenate()
                    && !inlineLC.getHyphContext().hasMoreHyphPoints()) {
                break;
            }
        } // end of while on child LM
        if ((curLM = getChildLM()) == null) {
            // No more content to layout!
            setFinished(true);
        }

        if (bp == null) {
            return null;
        }
        if (prevBP == null) {
            BreakPoss prevLineEnd = (iPrevLineEnd == 0)
                ? null
                : (BreakPoss) vecInlineBreaks.get(iPrevLineEnd);
            if (allAreSuppressible(prevLineEnd)) {
                removeAllBP(prevLineEnd);
                return null;
            } else {
                prevBP = bp;
            }
        }

        // Choose the best break
        if (!bp.isForcedBreak() && vecPossEnd.size() > 0) {
            prevBP = getBestBP(vecPossEnd);
        }
        // Backup child LM if necessary
        if (bp != prevBP && !prevCouldEndLine(prevBP)) {
            reset();
        }

        // Don't justify last line in the sequence or if forced line-end
        int talign = bTextAlignment;
        if ((bTextAlignment == TextAlign.JUSTIFY
                             && (prevBP.isForcedBreak()
                             || isFinished()))) {
            talign = TextAlign.START;
        }
        return makeLineBreak(iPrevLineEnd, availIPD, talign);
    }

    private void resetBP(BreakPoss resetBP) {
        if (resetBP == null) {
            reset((Position) null);
        } else {
            while (vecInlineBreaks.get(vecInlineBreaks.size() - 1) != resetBP) {
                vecInlineBreaks.remove(vecInlineBreaks.size() - 1);
            }
            reset(resetBP.getPosition());
        }
    }

    private void reset() {
        resetBP(prevBP);
    }

    protected boolean couldEndLine(BreakPoss bp) {
        if (bp.canBreakAfter()) {
            return true; // no keep, ends on break char
        } else if (bp.isSuppressible()) {
            // NOTE: except at end of content for this LM!!
            // Never break after only space chars or any other sequence
            // of areas which would be suppressed at the end of the line.
            return false;
        } else {
            // See if could break before next area
            // TODO: do we need to set anything on the layout context?
            LayoutContext lc = new LayoutContext(0);
            LayoutManager nextLM = getChildLM();
            return (nextLM == null || nextLM.canBreakBefore(lc));
        }
    }

    private BreakPoss getBestBP(ArrayList vecPossEnd) {
        if (vecPossEnd.size() == 1) {
            return ((BreakCost) vecPossEnd.get(0)).getBP();
        }
        // Choose the best break (use a sort on cost!)
        Iterator iter = vecPossEnd.iterator();
        int minCost = Integer.MAX_VALUE;
        BreakPoss bestBP = null;
        while (iter.hasNext()) {
            BreakCost bc = (BreakCost) iter.next();
            if (bc.getCost() < minCost) {
                minCost = bc.getCost();
                bestBP = bc.getBP();
            }
        }
        return bestBP;
    }

    /** Line area is always considered to act as a fence. */
    protected boolean hasLeadingFence(boolean bNotFirst) {
        return true;
    }

    /** Line area is always considered to act as a fence. */
    protected boolean hasTrailingFence(boolean bNotLast) {
        return true;
    }

    /** Test whether all breakposs in vecInlineBreaks
        back to and including prev could end line */
    private boolean prevCouldEndLine(BreakPoss prev) {
        ListIterator bpIter =
            vecInlineBreaks.listIterator(vecInlineBreaks.size());
        boolean couldEndLine = true;
        while (bpIter.hasPrevious()) {
            BreakPoss bp = (BreakPoss) bpIter.previous();
            couldEndLine = bp.couldEndLine();
            if (!couldEndLine || bp == prev) break;
        }
        return couldEndLine;
    }

    /** Test whether all breakposs in vecInlineBreaks
        back to and excluding prev are suppressible */
    private boolean allAreSuppressible(BreakPoss prev) {
        ListIterator bpIter =
            vecInlineBreaks.listIterator(vecInlineBreaks.size());
        boolean allAreSuppressible = true;
        BreakPoss bp;
        while (bpIter.hasPrevious()
               && (bp = (BreakPoss) bpIter.previous()) != prev
               && (allAreSuppressible = bp.isSuppressible())) {
        }
        return allAreSuppressible;
    }

    /** Remove all BPs from the end back to and excluding prev
        from vecInlineBreaks*/
    private void removeAllBP(BreakPoss prev) {
        int iPrev;
        if (prev == null) {
            vecInlineBreaks.clear();
        } else if ((iPrev = vecInlineBreaks.indexOf(prev)) != -1) {
            for (int i = vecInlineBreaks.size()-1; iPrev < i; --i) {
                vecInlineBreaks.remove(i);
            }
        }
    }

    private HyphContext getHyphenContext(BreakPoss prev,
                                         BreakPoss newBP) {
        // Get a "word" to hyphenate by getting characters from all
        // pending break poss which are in vecInlineBreaks, starting
        // with the position just AFTER prev.getPosition()

        vecInlineBreaks.add(newBP);
        ListIterator bpIter =
            vecInlineBreaks.listIterator(vecInlineBreaks.size());
        while (bpIter.hasPrevious() && bpIter.previous() != prev) {
        }
        if (prev != null && bpIter.next() != prev) {
            getLogger().error("findHyphenPoss: problem!");
            return null;
        }
        StringBuffer sbChars = new StringBuffer(30);
        while (bpIter.hasNext()) {
            BreakPoss bp = (BreakPoss) bpIter.next();
            if (prev != null &&
                bp.getLayoutManager() == prev.getLayoutManager()) {
                bp.getLayoutManager().getWordChars(sbChars,
                    prev.getPosition(), bp.getPosition());
            } else {
                bp.getLayoutManager().getWordChars(sbChars, null,
                    bp.getPosition());
            }
            prev = bp;
        }
        vecInlineBreaks.remove(vecInlineBreaks.size() - 1); // remove last
        getLogger().debug("Word to hyphenate: " + sbChars.toString());

        // Now find all hyphenation points in this word (get in an array of offsets)
        // hyphProps are from the block level?. Note that according to the spec,
        // they also "apply to" fo:character. I don't know what that means, since
        // if we change language in the middle of a "word", the effect would seem
        // quite strange! Or perhaps in that case, we say that it's several words.
        // We probably should bring the hyphenation props up from the actual
        // TextLM which generate the hyphenation buffer, since these properties
        // inherit and could be specified on an inline or wrapper below the block
        // level.
        Hyphenation hyph = Hyphenator.hyphenate(hyphProps.language,
                                                hyphProps.country, sbChars.toString(),
                                                hyphProps.hyphenationRemainCharacterCount,
                                                hyphProps.hyphenationPushCharacterCount);
        // They hyph structure contains the information we need
        // Now start from prev: reset to that position, ask that LM to get
        // a Position for the first hyphenation offset. If the offset isn't in
        // its characters, it returns null, but must tell how many chars it had.
        // Keep looking at currentBP using next hyphenation point until the
        // returned size is greater than the available size or no more hyphenation
        // points remain. Choose the best break.
        if (hyph != null) {
            return new HyphContext(hyph.getHyphenationPoints());
        } else {
            return null;
        }
    }

    /**
     * Make a line break for returning as the next break.
     * This makes the line break and calculates the height and
     * ipd adjustment factors.
     *
     * @param prevLineEnd previous line break index
     * @param target the target ipd value
     * @param textalign the text align in operation for this line
     * @return the line break position
     */
    private BreakPoss makeLineBreak(int prevLineEnd, MinOptMax target,
                                    int textalign) {
        // make a new BP
        // Store information needed to make areas in the LineBreakPosition!

        // lead to baseline is
        // max of: baseline fixed alignment and middle/2
        // after baseline is
        // max: top height-lead, middle/2 and bottom height-lead
        int halfLeading = (lineHeight - lead - follow) / 2;
        // height before baseline
        int lineLead = lead + halfLeading;
        // maximum size of top and bottom alignment
        int maxtb = follow + halfLeading;
        // max size of middle alignment below baseline
        int middlefollow = maxtb;

        // calculate actual ipd
        MinOptMax actual = new MinOptMax();
        BreakPoss lastBP = null;
        LayoutManager lastLM = null;
        for (Iterator iter = vecInlineBreaks.listIterator(prevLineEnd);
                iter.hasNext();) {
            BreakPoss bp = (BreakPoss)iter.next();
            if (bp.getLead() > lineLead) {
                lineLead = bp.getLead();
            }
            if (bp.getTotal() > maxtb) {
                maxtb = bp.getTotal();
            }
            if (bp.getMiddle() > middlefollow) {
                middlefollow = bp.getMiddle();
            }

            // the stacking size of textLM accumulate for each break
            // so the ipd is only added at the end of each LM
            if (bp.getLayoutManager() != lastLM) {
                if (lastLM != null) {
                    actual.add(lastBP.getStackingSize());
                }
                lastLM = bp.getLayoutManager();
            }
            lastBP = bp;
        }
        if (lastBP != null) {
            // add final ipd
            actual.add(lastBP.getStackingSize());
            // ATTENTION: make sure this hasn't gotten start space for next
            // LM added onto it!
            actual.add(lastBP.resolveTrailingSpace(true));
        }

        if (maxtb - lineLead > middlefollow) {
            middlefollow = maxtb - lineLead;
        }

        // in 7.21.4 the spec suggests that the leader and other
        // similar min/opt/max areas should be adjusted before
        // adjusting word spacing

        // Calculate stretch or shrink factor
        double ipdAdjust = 0;
        int targetWith = target.opt;
        int realWidth = actual.opt;
        if (actual.opt > targetWith) {
            if (actual.opt - targetWith < (actual.opt - actual.min)) {
                ipdAdjust = -(actual.opt - targetWith)
                                / (float)(actual.opt - actual.min);
                realWidth = targetWith;
            } else {
                ipdAdjust = -1;
                realWidth = actual.max;
            }
        } else {
            if (targetWith - actual.opt < actual.max - actual.opt) {
                ipdAdjust = (targetWith - actual.opt)
                                / (float)(actual.max - actual.opt);
                realWidth = targetWith;
            } else {
                ipdAdjust = 1;
                realWidth = actual.min;
            }
        }

        // if justifying then set the space adjustment
        // after the normal ipd adjustment
        double dAdjust = 0.0;
        int indent = 0;
        switch (textalign) {
            case TextAlign.JUSTIFY:
                if (realWidth != 0) {
                    dAdjust = (targetWith - realWidth) / realWidth;
                }
            break;
            case TextAlign.START:
                if (prevLineEnd == 0) {
                    indent = textIndent.getValue();
                }
                break;
            case TextAlign.CENTER:
                indent = (targetWith - realWidth) / 2;
            break;
            case TextAlign.END:
                indent = targetWith - realWidth;
            break;
        }

        LineBreakPosition lbp;
        lbp = new LineBreakPosition(this,
                                    vecInlineBreaks.size() - 1,
                                    ipdAdjust, dAdjust, indent,
                                    lineLead + middlefollow, lineLead);
        BreakPoss curLineBP = new BreakPoss(lbp);

        curLineBP.setFlag(BreakPoss.ISLAST, isFinished());
        curLineBP.setStackingSize(new MinOptMax(lineLead + middlefollow));
        return curLineBP;
    }

    /**
     * Reset the positions to the given position.
     *
     * @param resetPos the position to reset to
     */
    public void resetPosition(Position resetPos) {
        if (resetPos == null) {
            iStartPos = 0;
            reset(null);
            vecInlineBreaks.clear();
            prevBP = null;
        } else {
            prevBP = (BreakPoss)vecInlineBreaks.get(((LineBreakPosition)resetPos).getLeafPos());
            while (vecInlineBreaks.get(vecInlineBreaks.size() - 1) != prevBP) {
                vecInlineBreaks.remove(vecInlineBreaks.size() - 1);
            }
            reset(prevBP.getPosition());
        }
    }

    /**
     * Add the areas with the break points.
     *
     * @param parentIter the iterator of break positions
     * @param context the context for adding areas
     */
    public void addAreas(PositionIterator parentIter,
                         LayoutContext context) {
        addAreas(parentIter, 0.0);

        //vecInlineBreaks.clear();
        prevBP = null;
    }

    // Generate and add areas to parent area
    // Set size etc
    // dSpaceAdjust should reference extra space in the BPD
    /**
     * Add the areas with the associated space adjustment.
     *
     * @param parentIter the iterator of breaks positions
     * @param dSpaceAdjust the space adjustment
     */
    public void addAreas(PositionIterator parentIter, double dSpaceAdjust) {
        LayoutManager childLM;
        LayoutContext lc = new LayoutContext(0);
        while (parentIter.hasNext()) {
            LineBreakPosition lbp = (LineBreakPosition) parentIter.next();
            LineArea lineArea = new LineArea();
            lineArea.setStartIndent(lbp.startIndent);
            lineArea.setHeight(lbp.lineHeight);
            lc.setBaseline(lbp.baseline);
            lc.setLineHeight(lbp.lineHeight);
            setCurrentArea(lineArea);
            // Add the inline areas to lineArea
            PositionIterator inlinePosIter =
              new BreakPossPosIter(vecInlineBreaks, iStartPos,
                                   lbp.getLeafPos() + 1);
            iStartPos = lbp.getLeafPos() + 1;
            lc.setSpaceAdjust(lbp.dAdjust);
            lc.setIPDAdjust(lbp.ipdAdjust);
            lc.setLeadingSpace(new SpaceSpecifier(true));
            lc.setTrailingSpace(new SpaceSpecifier(false));
            lc.setFlags(LayoutContext.RESOLVE_LEADING_SPACE, true);
            setChildContext(lc);
            while ((childLM = inlinePosIter.getNextChildLM()) != null) {
                childLM.addAreas(inlinePosIter, lc);
                lc.setLeadingSpace(lc.getTrailingSpace());
                lc.setTrailingSpace(new SpaceSpecifier(false));
            }
            // when can this be null?
            if (lc.getTrailingSpace() != null) {
                addSpace(lineArea, lc.getTrailingSpace().resolve(true),
                         lc.getSpaceAdjust());
            }
            parentLM.addChild(lineArea);
        }
        setCurrentArea(null); // ?? necessary
    }

    /**
     * Add an unresolved area.
     * If a child layout manager needs to add an unresolved area
     * for page reference or linking then this intercepts it for
     * line area handling.
     * A line area may need to have the inline areas adjusted
     * to properly fill the line area. This adds a resolver that
     * resolves the inline area and can do the necessary
     * adjustments to the line and inline areas.
     *
     * @param id the id reference of the resolveable
     * @param res the resolveable object
     */
    public void addUnresolvedArea(String id, Resolveable res) {
        // create a resolveable class that handles ipd
        // adjustment for the current line

        parentLM.addUnresolvedArea(id, res);
    }

}

