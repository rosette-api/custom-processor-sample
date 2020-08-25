/*******************************************************************************
  ** This data and information is proprietary to, and a valuable trade secret
  ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
  ** and may only be used as permitted under the license agreement under which
  ** it has been distributed, and in no other way.
  **
  ** Copyright (c) 2017 Basis Technology Corporation All rights reserved.
  **
  ** The technical data and information provided herein are provided with
  ** `limited rights', and the computer software provided herein is provided
  ** with `restricted rights' as those terms are defined in DAR and ASPR
  ** 7-104.9(a).
 ******************************************************************************/

package sample;

import com.basistech.rosette.dm.AnnotatedText;
import com.basistech.rosette.dm.Annotator;
import com.basistech.rosette.dm.Entity;
import com.basistech.rosette.dm.ListAttribute;
import com.basistech.rosette.dm.Mention;
import com.basistech.rosette.rex.custom.CustomProcessor;
import com.basistech.rosette.rex.custom.Phase;
import com.basistech.util.LanguageCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * <code>SampleCustomProcessor</code> is a sample <code>CustomProcessor</code> implementation.
 * The <code>SampleCustomProcessor</code> provides three annotators for different processing phases.
 */
public class SampleCustomProcessor implements CustomProcessor {

    @Override
    public Annotator createAnnotator(String name, LanguageCode language, Phase phase) {
        // name, supported language, and running phase
        if ("personContextAnnotator".equals(name) && language == LanguageCode.ENGLISH && phase == Phase.preRedaction) {
            return new PersonContextAnnotator();
        }
        if ("boundaryAdjustAnnotator".equals(name) && language == LanguageCode.ENGLISH && phase == Phase.preRedaction) {
            return new BoundaryAdjustAnnotator();
        }
        // name and running phase. this annotator is available for any language
        if ("metadataAnnotator".equals(name) && phase == Phase.preExtraction) {
            return new MetadataAnnotator();
        }
        return null;
    }


    /**
     * <code>PersonContextAnnotator</code> runs at <code>preRedaction</code> phase to update entity mention type to
     * person type in a case where the mention was found in a context of a person.
     */
    private static class PersonContextAnnotator implements Annotator {
        @Override
        public AnnotatedText annotate(CharSequence input) {
            throw new RuntimeException("Not supported");
        }

        /**
         * Annotates text after extraction processors run, and before <code>redactor</code> and <code>indocCoref</code>
         * processors run.
         * @param input - annotated text (ADM), which contains a list of entities (E0, E1, E2,...) where each entry is
         *              considered to be the first (and only) mention of an entity (i.e., M0), because indoc coref has
         *              not yet been run to chain together multiple mentions of the same entity.
         * @return AnnotatedText annotated text with modified entity list
         */
        @SuppressWarnings("unchecked")
        @Override
        public AnnotatedText annotate(AnnotatedText input) {
            Set<String[]> personContexts = new HashSet<>();
            personContexts.add(new String[]{"Love, ", "\n"});
            personContexts.add(new String[]{"My name is ", ""});

            ListAttribute.Builder newEntities = new ListAttribute.Builder(Entity.class);
            String data = input.getData().toString();

            for (Entity e : input.getEntities()) {
                // at "preRedaction" phase each entity contains a single mention
                Mention m = e.getMentions().get(0);
                boolean hasPersonContext = false;
                for (String[] personContext : personContexts) {
                    if (data.substring(0, m.getStartOffset()).endsWith(personContext[0])
                            && data.substring(m.getEndOffset()).startsWith(personContext[1])) {
                        hasPersonContext = true;
                        break;
                    }
                }
                if (hasPersonContext) {
                    e = updateType("PersonContextAnnotator", e, "PERSON");
                }
                newEntities.add(e);
            }
            return new AnnotatedText.Builder(input).entities(newEntities.build()).build();
        }
    }

    /**
     * <code>BoundaryAdjustAnnotator</code> runs at <code>preRedaction</code> phase, adjusting boundaries for entity
     * mentions that incorrectly included extra tokens that are clearly not part of any entity mention.
     */
    private static class BoundaryAdjustAnnotator implements Annotator {
        @Override
        public AnnotatedText annotate(CharSequence input) {
            throw new RuntimeException("Not supported");
        }

        /**
         * Annotates text after extraction processors run, and before <code>redactor</code> and <code>indocCoref</code>
         * processors run.
         * @param input - annotated text (ADM), which contains a list of entities (E0, E1, E2,...) where each entry is
         *              considered to be the first (and only) mention of an entity (i.e., M0), because indoc coref has
         *              not yet been run to chain together multiple mentions of the same entity.
         * @return AnnotatedText annotated text with modified entity list
         */
        @SuppressWarnings("unchecked")
        @Override
        public AnnotatedText annotate(AnnotatedText input) {
            List<String> invalidPrefixes = new ArrayList<>();
            invalidPrefixes.add("Hi ");
            invalidPrefixes.add("Hey ");

            ListAttribute.Builder newEntities = new ListAttribute.Builder(Entity.class);
            for (Entity e : input.getEntities()) {
                // at "preRedaction" phase each entity contains a single mention
                Mention m = e.getMentions().get(0);
                String invalidPrefix = "";
                for (String invalidPrefixCandidate : invalidPrefixes) {
                    if (m.getNormalized().startsWith(invalidPrefixCandidate)) {
                        invalidPrefix = invalidPrefixCandidate;
                        break;
                    }
                }

                if (!invalidPrefix.isEmpty()) {
                    int prefixLength = invalidPrefix.length();
                    e = updateText("PersonContextAnnotator", e, m.getNormalized().substring(prefixLength),
                            m.getStartOffset() + prefixLength, m.getEndOffset());
                }

                newEntities.add(e);
            }
            return new AnnotatedText.Builder(input).entities(newEntities.build()).build();
        }
    }

    private static class MetadataAnnotator implements Annotator {
        @Override
        public AnnotatedText annotate(CharSequence input) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public AnnotatedText annotate(AnnotatedText input) {
            return new AnnotatedText.Builder(input)
                    .documentMetadata("custom", "custom data")
                    .build();
        }
    }

    static Entity updateText(String annotator, Entity e, String text, int startOffset, int endOffset) {
        Mention m = e.getMentions().get(0);
        return new Entity.Builder().type(e.getType())
                .entityId(e.getEntityId())
                .confidence(e.getConfidence())
                .mention(new Mention.Builder(m).normalized(text)
                        .startOffset(startOffset).endOffset(endOffset)
                        .source("SampleCustomProcessor")
                        .subsource(annotator).build())
                .build();
    }

    static Entity updateType(String annotator, Entity e, String type) {
        Mention m = e.getMentions().get(0);
        return new Entity.Builder().type(type)
                .entityId(e.getEntityId())
                .confidence(e.getConfidence())
                .mention(new Mention.Builder(m)
                        .source("SampleCustomProcessor")
                        .subsource(annotator).build())
                .build();
    }
}
