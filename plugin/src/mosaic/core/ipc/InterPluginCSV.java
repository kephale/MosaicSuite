package mosaic.core.ipc;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.io.dozer.ICsvDozerBeanReader;
import org.supercsv.io.dozer.ICsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;


public class InterPluginCSV<E> {
    protected static final Logger logger = Logger.getLogger(InterPluginCSV.class);
    
    final private Class<E> iClazz;
    private CsvPreference iCsvPreference;
    final private Vector<MetaInfo> iMetaInfos;
    final private Vector<MetaInfo> iMetaInfosRead;

    /**
     * @param aClazz E.class
     */
    public InterPluginCSV(Class<E> aClazz) {
        iClazz = aClazz;
        iMetaInfos = new Vector<MetaInfo>();
        iMetaInfosRead = new Vector<MetaInfo>();
        setCsvPreference(',');
    }

    /**
     * Set Meta information
     * @param aParameter meta information
     * @param aValue of the meta information
     */
    public void setMetaInformation(String aParameter, String aValue) {
        setMetaInformation(new MetaInfo(aParameter, aValue));
    }
    
    /**
     * Set Meta information
     * @param aMetaInfo - meta information
     */
    public void setMetaInformation(MetaInfo aMetaInfo) {
        iMetaInfos.add(aMetaInfo);
    }

    /**
     * Get Meta information
     * 
     * @param parameter - Name of meta information parameter
     * @return Value of the meta information or null if not found
     */
    public String getMetaInformation(String parameter) {
        for (MetaInfo mi : iMetaInfos) {
            if (mi.parameter.equals(parameter)) return mi.value;
        }
        for (MetaInfo mi : iMetaInfosRead) {
            if (mi.parameter.equals(parameter)) return mi.value;
        }
    
        return null;
    }

    /**
     * Remove Meta Information
     * @param parameter - Name of meta information parameter
     */
    public void removeMetaInformation(String parameter) {
        for (int i = 0; i < iMetaInfos.size(); i++) {
            if (iMetaInfos.get(i).parameter.equals(parameter)) {
                iMetaInfos.remove(i);
                break;
            }
        }

        for (int i = 0; i < iMetaInfosRead.size(); i++) {
            if (iMetaInfosRead.get(i).parameter.equals(parameter)) {
                iMetaInfosRead.remove(i);
                break;
            }
        }
    }

    /**
     * Delete all previously set meta information
     */
    public void clearMetaInformation() {
        iMetaInfos.clear();
    }

    /**
     * Trying to figure out the best setting to read the CSV file
     * @param aCsvFilename
     */
    public void setCSVPreferenceFromFile(String aCsvFilename) {
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(aCsvFilename), iCsvPreference);

            String[] map = beanReader.getHeader(true);
            // In case when header map is equal to 1 it is probable that current delimiter is not correct
            // Try to find better. (Of course there is always a chanse that there is only one column).
            if (map.length == 1) {
                if (map[0].split(";").length > 1) {
                    setDelimiter(';');
                }
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reads a CSV file
     * 
     * @param aCsvFilename Name of the filename to open
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename) {
        return Read(aCsvFilename, null);
    }

    /**
     * Read a CSV file
     * 
     * @param aCsvFilename - Name of the filename to open
     * @param OutputChoose - output choose with defined colum names and processors
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename, OutputChoose aOutputChoose) {
        Vector<E> out = new Vector<E>();
        readData(aCsvFilename, out, aOutputChoose);

        return out;
    }

    /**
     * Writes data to csv file.
     * @param aCsvFilename - full absolute path/name of output file
     * @param aOutputData - container with data to be written
     * @param aOutputChoose - names/processors of expected output
     * @param aShouldAppend - if appends, then header and metainformation is not written
     */
    public void Write(String aCsvFilename, List<E> aOutputData, OutputChoose aOutputChoose, boolean aShouldAppend) {
        // Make sure that OutputChoose does not contain empty (null) values for header
        List<String> map = new ArrayList<String>();
        List<CellProcessor> cp = new ArrayList<CellProcessor>();
        boolean isErrorReported = false;
        for (int i = 0; i < aOutputChoose.map.length; ++i) {
            if (aOutputChoose.map[i] != null && !aOutputChoose.map[i].equals("")) {
                map.add(aOutputChoose.map[i]);
                cp.add(aOutputChoose.cel[i]);
            }
            else {
                if (!isErrorReported) {
                    logger.error("Empty or null [" + aOutputChoose.map[i] + "] field declared for file [" + aCsvFilename + "]!");
                    logger.error(aOutputChoose);
                    isErrorReported = true;
                }
            }
        }
        String[] mapString = map.toArray(new String[map.size()]);
        CellProcessor[] cel = cp.toArray(new CellProcessor[cp.size()]);
        OutputChoose oc = new OutputChoose(mapString, cel);
        
        writeData(aCsvFilename, aOutputData, oc, aShouldAppend);  
    }
        
    /**
     * Set delimiter
     * @param d delimiter
     */
    public void setDelimiter(char d) {
        setCsvPreference(d);
    }

    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion)
     * 
     * @param aInputFileNames - files to stitch
     * @param aOutputFileName - output stitched file
     * @return true when succeed
     */
    public boolean Stitch(String[] aInputFileNames, String aOutputFileName) {
        return Stitch(aInputFileNames, aOutputFileName, null);
    }
    
    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion)
     * 
     * @param aInputFileNames - files to stitch
     * @param aOutputFileName - output stitched file
     * @return true when succeed
     */
    public boolean Stitch(String[] aInputFileNames, String aOutputFileName, OutputChoose aOutputChoose) {
        if (aInputFileNames.length == 0) return false;
        
        Vector<E> out = new Vector<E>();
        OutputChoose occ = readData(aInputFileNames[0], out, aOutputChoose);
        if (occ == null) return false;
    
        for (int i = 1; i < aInputFileNames.length; i++) {
            readData(aInputFileNames[i], out, occ);
        }
    
        Write(aOutputFileName, out, occ, false);
    
        return true;
    }

    /**
     * Read a CSV file
     * 
     * @param aCsvFilename - CSV filename
     * @param out output - container for output data
     * @param aOutputChoose - chosen output (if null, it will be generated from header)
     */
    OutputChoose readData(String aCsvFilename, Vector<E> aOutput, OutputChoose aOutputChoose) {
        ICsvDozerBeanReader beanReader = null;
        try {
            logger.info("Reading file: [" + aCsvFilename + "]");
            beanReader = new CsvDozerBeanReader(new FileReader(aCsvFilename), iCsvPreference);
    
            String[] map = beanReader.getHeader(true);
            if (map == null) return null; // we cannot get the header
            if (aOutputChoose == null) aOutputChoose = generateOutputChoose(map);

            beanReader.configureBeanMapping(iClazz, aOutputChoose.map);
    
            E element;
            while ((element = beanReader.read(iClazz, aOutputChoose.cel)) != null) {
                aOutput.add(element);
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return aOutputChoose;
    }

    private void writeData(String aCsvFilename, List<E> aOutputData, OutputChoose aOutputChoose, boolean aShouldAppend) {
        if (aOutputData.size() == 0) return;
    
        ICsvDozerBeanWriter beanWriter = null;
        try {
            logger.info("Writing file: [" + aCsvFilename + "]");
            beanWriter = new CsvDozerBeanWriter(new FileWriter(aCsvFilename, aShouldAppend), iCsvPreference);
            beanWriter.configureBeanMapping(iClazz, aOutputChoose.map);
    
            // write the header and meta information
            if (aShouldAppend == false) {
                beanWriter.writeHeader(aOutputChoose.map);
    
                // Write meta information
                for (MetaInfo mi : iMetaInfos) {
                    beanWriter.writeComment("%" + mi.parameter + ":" + mi.value);
                }
    
                // write read meta information if specified
                for (MetaInfo mi : iMetaInfosRead) {
                    beanWriter.writeComment("%" + mi.parameter + ":" + mi.value);
                }
            }
    
            // write the beans
            try {
    
                for (int i = 0; i < aOutputData.size(); i++) {
                    beanWriter.write(aOutputData.get(i), aOutputChoose.cel);
                }
                
            } catch (SecurityException e) {
                e.printStackTrace();
                return;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (beanWriter != null) {
                try {
                    beanWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Generates OutputChoose from provided header keywords.
     * @param aHeaderKeywords - array of keywords strings
     * @return generated OutputChoose
     */
    private OutputChoose generateOutputChoose(String[] aHeaderKeywords) {
        CellProcessor c[] = new CellProcessor[aHeaderKeywords.length];

        for (int i = 0; i < c.length; i++) {
            try {
                aHeaderKeywords[i] = aHeaderKeywords[i].replace(" ", "_");
                c[i] = null;
                
                iClazz.getMethod("get" + aHeaderKeywords[i]);
            } catch (NoSuchMethodException e) {
                // getProcessor from above get(MethodName) is not existing
                logger.info("Method not found: [" + "get" + aHeaderKeywords[i] + "], setting to default (ignore) setup. Class: " + iClazz.getName());
                c[i] = null;
                aHeaderKeywords[i] = null;
                continue;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        
        OutputChoose occ = new OutputChoose(aHeaderKeywords, c);
        logger.debug("Generated field mapping: " + occ);
        
        return occ;
    }
    
    private class CommentExtendedCSV implements CommentMatcher {
        @Override
        public boolean isComment(String s) {
            // Comment style:
            // %parameter:value
            if (s.startsWith("%")) {
                String[] pr = s.split(":");

                if (pr.length == 2) {
                    iMetaInfosRead.add(new MetaInfo(pr[0].substring(1), pr[1].trim()));
                }
                return true;
            }
            return false;
        }
    }
    
    private void setCsvPreference(char aDelimiter) {
        CsvPreference.Builder bld = new CsvPreference.Builder('"', aDelimiter, "\n");
        bld.skipComments(new CommentExtendedCSV());
        iCsvPreference = bld.build();
    }
}
