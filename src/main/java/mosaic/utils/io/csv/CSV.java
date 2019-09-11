package mosaic.utils.io.csv;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class CSV<E> {
    protected static final Logger logger = Logger.getLogger(CSV.class);

    private final Class<E> iClazz;
    private CsvPreference iCsvPreference;
    private final Vector<CsvMetaInfo> iMetaInfos;
    final Vector<CsvMetaInfo> iMetaInfosRead;

    /**
     * @param aClazz E.class
     */
    public CSV(Class<E> aClazz) {
        iClazz = aClazz;
        iMetaInfos = new Vector<CsvMetaInfo>();
        iMetaInfosRead = new Vector<CsvMetaInfo>();
        setCsvPreference(',');
    }

    /**
     * Set Meta information
     * @param aParameter meta information
     * @param aValue of the meta information
     */
    public void setMetaInformation(String aParameter, String aValue) {
        setMetaInformation(new CsvMetaInfo(aParameter, aValue));
    }

    /**
     * Set Meta information
     * @param aMetaInfo - meta information
     */
    public void setMetaInformation(CsvMetaInfo aMetaInfo) {
        final String value = getMetaInformation(aMetaInfo.parameter);
        if (value != null) {
            logger.debug("MetaInfo " + aMetaInfo + " added, but same parameter with value [" + value + "] already exists!");
        }
        iMetaInfos.add(aMetaInfo);
    }

    /**
     * Get Meta information
     *
     * @param parameter - Name of meta information parameter
     * @return Value of the meta information or null if not found
     */
    public String getMetaInformation(String parameter) {
        String value = getMetaInformation(iMetaInfos, parameter);
        if (value == null) {
            value = getMetaInformation(iMetaInfosRead, parameter);
        }
        return value;
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
        iMetaInfosRead.clear();
    }

    /**
     * Trying to figure out the best setting to read the CSV file
     * @param aCsvFilename
     * @return 
     */
    public int setCSVPreferenceFromFile(String aCsvFilename) {
        //ICsvDozerBeanReader beanReader = null;
        int numOfColumns = 0;

        return numOfColumns;
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
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename, CsvColumnConfig aOutputChoose) {
        return Read(aCsvFilename, aOutputChoose, false);
    }
    
    /**
     * Read a CSV file
     *
     * @param aCsvFilename - Name of the filename to open
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename, CsvColumnConfig aOutputChoose, boolean aSkipHeader) {
        final Vector<E> out = new Vector<E>();
        readData(aCsvFilename, out, aOutputChoose, aSkipHeader);

        return out;
    }

    /**
     * Writes data to csv file.
     * @param aCsvFilename - full absolute path/name of output file
     * @param aOutputData - container with data to be written
     * @param aOutputChoose - names/processors of expected output
     * @param aShouldAppend - if appends, then header and metainformation is not written
     */
    public boolean Write(String aCsvFilename, List<E> aOutputData, CsvColumnConfig aOutputChoose, boolean aShouldAppend) {
        // Make sure that OutputChoose does not contain empty (null) values for header
        final List<String> map = new ArrayList<String>();
        final List<CellProcessor> cp = new ArrayList<CellProcessor>();
        boolean isErrorReported = false;
        for (int i = 0; i < aOutputChoose.fieldMapping.length; ++i) {
            if (aOutputChoose.fieldMapping[i] != null && !aOutputChoose.fieldMapping[i].equals("")) {
                map.add(aOutputChoose.fieldMapping[i]);
                cp.add(aOutputChoose.cellProcessors[i]);
            }
            else {
                if (!isErrorReported) {
                    logger.error("Empty or null [" + aOutputChoose.fieldMapping[i] + "] field declared for file [" + aCsvFilename + "]!");
                    logger.error(aOutputChoose);
                    isErrorReported = true;
                }
            }
        }
        final String[] mapString = map.toArray(new String[map.size()]);
        final CellProcessor[] cel = cp.toArray(new CellProcessor[cp.size()]);
        final CsvColumnConfig oc = new CsvColumnConfig(mapString, cel);

        return writeData(aCsvFilename, aOutputData, oc, aShouldAppend);
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
    boolean Stitch(String[] aInputFileNames, String aOutputFileName, CsvColumnConfig aOutputChoose) {
        if (aInputFileNames.length == 0) {
            return false;
        }

        final Vector<E> out = new Vector<E>();
        final CsvColumnConfig occ = readData(aInputFileNames[0], out, aOutputChoose, false);
        if (occ == null) {
            return false;
        }

        for (int i = 1; i < aInputFileNames.length; i++) {
            readData(aInputFileNames[i], out, occ, false);
        }

        Write(aOutputFileName, out, occ, false);

        return true;
    }

    /**
     * Stitch CSV files in one with an unknown (but equal between files) format. Can handle any header(s) and value(s) since 
     * they are not interpreted (so provided type class does not matter here). There is also no verification that all files are 
     * same (in terms of header/columns).
     * @param aInputFileNames - files to stitch
     * @param aOutputFileName - output stitched file
     */
    public boolean StitchAny(String[] aInputFileNames, String aOutputFileName) {
        if (aInputFileNames.length == 0) {
            return false;
        }
        
        try {
            CellProcessor[] processors = null;
            String[] map = null;
            
            // First step - read all files. 
            List<List<Object>> allValues = new ArrayList<List<Object>>();
            for (int i = 0; i < aInputFileNames.length; i++) {
                ICsvListReader listReader = null;
                try {
                    listReader = new CsvListReader(new FileReader(aInputFileNames[i]), iCsvPreference);
                    map = listReader.getHeader(true);
                    processors = new CellProcessor[map.length];
                    for (int p = 0; p < processors.length; p++) processors[p] = new NotNull();

                    List<Object> value;
                    while( (value = listReader.read(processors)) != null ) allValues.add(value);
                }
                finally {
                    if( listReader != null ) {
                        listReader.close();
                    }
                }
            }

            // Second step - save all readed stuff.
            ICsvListWriter listWriter = null;
            try {
                listWriter = new CsvListWriter(new FileWriter(aOutputFileName), iCsvPreference);

                listWriter.writeHeader(map);

                // write read meta information if specified
                for (final CsvMetaInfo mi : iMetaInfos) {
                    listWriter.writeComment("%" + mi.parameter + ":" + mi.value);
                }
                for (final CsvMetaInfo mi : iMetaInfosRead) {
                    listWriter.writeComment("%" + mi.parameter + ":" + mi.value);
                }

                for (List<Object> l : allValues) listWriter.write(l, processors);
            }
            finally {
                if (listWriter != null ) {
                    listWriter.close();
                }
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }

    /**
     * Read a CSV file
     *
     * @param aCsvFilename - CSV filename
     * @param aOutputChoose - chosen output (if null, it will be generated from header)
     */
    private CsvColumnConfig readData(String aCsvFilename, Vector<E> aOutput, CsvColumnConfig aOutputChoose, boolean aSkipHeader) {

        return aOutputChoose;
    }

    private boolean writeData(String aCsvFilename, List<E> aOutputData, CsvColumnConfig aOutputChoose, boolean aShouldAppend) {
        if (aOutputData.size() == 0) {
            logger.info("Nothing to write! File: [" + aCsvFilename + "] not created.");
            return false;
        }

        return true;
    }

    String getMetaInformation(List<CsvMetaInfo> aContainer, String aParameter) {
        for (final CsvMetaInfo mi : aContainer) {
            if (mi.parameter.equals(aParameter)) {
                return mi.value;
            }
        }
        return null;
    }

    /**
     * Generates OutputChoose from provided header keywords.
     * @param aHeaderKeywords - array of keywords strings
     * @return generated OutputChoose
     */
    private CsvColumnConfig generateOutputChoose(String[] aHeaderKeywords) {
        final CellProcessor c[] = new CellProcessor[aHeaderKeywords.length];

        for (int i = 0; i < c.length; i++) {
            try {
                aHeaderKeywords[i] = aHeaderKeywords[i].replace(" ", "_");
                c[i] = null;

                iClazz.getMethod("get" + aHeaderKeywords[i]);
            } catch (final NoSuchMethodException e) {
                // getProcessor from above get(MethodName) is not existing
                logger.info("Method not found: [" + "get" + aHeaderKeywords[i] + "], setting to default (ignore) setup. Class: " + iClazz.getName());
                aHeaderKeywords[i] = null;
                continue;
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            } catch (final SecurityException e) {
                e.printStackTrace();
            }
        }

        final CsvColumnConfig occ = new CsvColumnConfig(aHeaderKeywords, c);
        logger.debug("Generated field mapping: " + occ);

        return occ;
    }

    private class CommentExtendedCSV implements CommentMatcher {
        protected CommentExtendedCSV() {}
        
        @Override
        public boolean isComment(String s) {
            // Comment style:
            // %parameter:value
            if (s.startsWith("%")) {
                final String[] pr = s.split(":");

                if (pr.length >= 2) {
                    // Skip first sign "%"
                    String param = pr[0].substring(1);
                    // Parameter value is all after ":". Trim it to remove unneeded spaces.
                    String value = s.substring(pr[0].length() + 1, s.length()).trim();
                    final CsvMetaInfo mi = new CsvMetaInfo(param, value);
                    final String currentValue = getMetaInformation(iMetaInfosRead, mi.parameter);
                    if (currentValue != null) {
                        logger.debug("MetaInfo " + mi + " added, but same parameter with value [" + value + "] already exists!");
                    }
                    iMetaInfosRead.add(mi);
                }
                return true;
            }
            return false;
        }
    }

    private void setCsvPreference(char aDelimiter) {
        final CsvPreference.Builder bld = new CsvPreference.Builder('"', aDelimiter, "\n");
        bld.skipComments(new CommentExtendedCSV());
        iCsvPreference = bld.build();
    }
}
