package org.snpeff.proteome;

import org.snpeff.interval.Gene;
import org.snpeff.interval.Transcript;
import org.snpeff.snpEffect.VariantEffect;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProteinXml {

    private static int xmlDepth = 0;

    public static void writeProteinXml(String xmlProtLocation, String organism, HashMap<Transcript, List<VariantEffect>> transcriptVariants, List<String> sampleNames) throws IOException, XMLStreamException {
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileWriter(xmlProtLocation));
        writer.writeStartDocument();
        writeStartElement(writer, "mzLibProteinDb");

        for (Transcript tr : transcriptVariants.keySet())
        {
            if (!tr.isProteinCoding() || tr.protein().isEmpty()  || tr.isErrorStartCodon() || !tr.protein().contains("*"))
                continue;

            writeStartElement(writer, "entry");
            writeStartElement(writer, "accession");
            writer.writeCharacters(tr.getId());
            writer.writeEndElement(); xmlDepth--;

            if (tr.getId() != null) {
                writeStartElement(writer, "name");
                writer.writeCharacters(tr.getId());
                writer.writeEndElement(); xmlDepth--;
            }

            Gene gene = (Gene)tr.findParent(Gene.class);
            String name = gene.getGeneName();
            String id = gene.getId();
            String primary = name != null ? name : id;
            writeStartElement(writer, "gene");
            writeStartElement(writer, "name");
            writer.writeAttribute("type", "primary");
            writer.writeCharacters(primary);
            writer.writeEndElement(); xmlDepth--;
            writeStartElement(writer, "name");
            writer.writeAttribute("type", "accession");
            writer.writeCharacters(id);
            writer.writeEndElement(); xmlDepth--;
            writer.writeEndElement(); xmlDepth--;

            if (organism != null) {
                writeStartElement(writer, "organism");
                writeStartElement(writer, "name");
                writer.writeAttribute("type", "scientific");
                writer.writeCharacters(organism);
                writer.writeEndElement(); xmlDepth--;
                writer.writeEndElement(); xmlDepth--;
            }

            // TODO: I can't get at VcfGenotype from Variant, so will need to do that in Spritz. (Tried including VcfEntry in Variant initialization, and that back reference didn't work...)
            // TODO: I can also do the depth stuff there.
            // That would also make this output more compatible with UniProt
            // TODO: Implement a better Transcript.protein() method for this output: trim to stop codon and possibly go beyond coding domain if there's a frameshift that extends
            for (VariantEffect var : transcriptVariants.get(tr))
            {
                if (var.getFunctionalClass().compareTo(VariantEffect.FunctionalClass.MISSENSE) < 0) continue; // only annotate nonsynonymous variations

                writeStartElement(writer, "feature");
                writer.writeAttribute("type", "sequence variant");
                writer.writeAttribute("description", var.toString());
                writeStartElement(writer, "reference");
                writer.writeAttribute("allele", var.getVariant().getReference());
                writer.writeCharacters(var.getAaRef());
                writer.writeEndElement(); xmlDepth--; // reference
                writeStartElement(writer, "alternate");
                writer.writeAttribute("allele", var.getVariant().getAlt());
                writer.writeCharacters(var.getAaAlt());
                writer.writeEndElement(); xmlDepth--; // alternate

//                int gtCount = 0;
//                for (VcfGenotype gt : var.getVariant().getVcfEntry()){
//                    writeStartElement(writer, "genotype");
//                    writer.writeAttribute("sample", sampleNames.get(gtCount));
//                    writer.writeCharacters(gt.get("GT"));
//                    writer.writeEndElement(); xmlDepth--; // reference
//                    writeStartElement(writer, "alleleDepth");
//                    writer.writeAttribute("sample", sampleNames.get(gtCount));
//                    writer.writeCharacters(gt.get(VcfGenotype.GT_FIELD_ALLELIC_DEPTH_OF_COVERAGE));
//                    writer.writeEndElement(); xmlDepth--; // reference
//                    gtCount++;
//                }

                writeStartElement(writer, "location");
                if (var.getAaNetChange() != null)
                {
                    writeStartElement(writer, "position");
                    writer.writeAttribute("position", Integer.toString(var.getCodonNum()));
                    writer.writeEndElement(); xmlDepth--;
                }
                else
                {
                    writeStartElement(writer, "begin");
                    writer.writeAttribute("position", Integer.toString(var.getCodonNum()));
                    writer.writeEndElement(); xmlDepth--;
                    writeStartElement(writer, "end");
                    writer.writeAttribute("position", Integer.toString(var.getCodonNum() + var.getAaNetChange().length() - 1));
                    writer.writeEndElement(); xmlDepth--;
                }
                writer.writeEndElement(); xmlDepth--; // location
                xmlDepth--; writePretty(writer); writer.writeEndElement();  // feature
            }

            writeStartElement(writer, "sequence");
            writer.writeAttribute("length", Integer.toString(tr.protein().length()));
            writer.writeCharacters(tr.proteinTrimmed());
            writer.writeEndElement(); xmlDepth--; // sequence
            xmlDepth--; writePretty(writer); writer.writeEndElement(); // entry
        }
        xmlDepth--; writePretty(writer); writer.writeEndElement(); // mzLibProteinDb
        writer.writeEndDocument();
        writer.flush();
    }

    private static void writePretty(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters("\n");
        for (int x = 0; x < xmlDepth; x++){
            writer.writeCharacters("  ");
        }
    }

    private static void writeStartElement(XMLStreamWriter writer, String localName) throws XMLStreamException {
        writePretty(writer);
        writer.writeStartElement(localName);
        xmlDepth++;
    }
}
