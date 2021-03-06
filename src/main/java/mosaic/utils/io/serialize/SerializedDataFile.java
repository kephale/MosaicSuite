package mosaic.utils.io.serialize;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 * Class for serializing objects using standard serialization Java mechanism.
 * @param <T> type of object to (un)serialize
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SerializedDataFile<T extends Serializable> implements DataFile<T> {

    private static final Logger logger = Logger.getLogger(SerializedDataFile.class);

    @Override
    public boolean SaveToFile(String aSerializedFileName, T aObject2Save) {
        logger.debug("SaveToFile ["+ aSerializedFileName +"]");
        
        boolean ret = false;
        FileOutputStream fileOutput = null;
        ObjectOutputStream objectOutput = null;
        try {
            fileOutput = new FileOutputStream(aSerializedFileName);
            objectOutput = new ObjectOutputStream(fileOutput);
            objectOutput.writeObject(aObject2Save);
            ret = true;
        } catch (final FileNotFoundException e) {
            logger.debug("File [" + aSerializedFileName + "] cannot be written!");
            logger.error(ExceptionUtils.getStackTrace(e));
        } catch (final IOException e) {
            logger.error("An error occured during writing serialized file [" + aSerializedFileName + "]");
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            close(objectOutput);
            close(fileOutput);
        }
        
        return ret;
    }

    @Override
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz) {
        logger.debug("LoadFromFile [" + aSerializedFileName + "]");

        T ret = null;
        FileInputStream fileInput = null;
        ObjectInputStream objectInput = null;
        try {
            fileInput = new FileInputStream(aSerializedFileName);
            objectInput = new ObjectInputStream(fileInput);
            Object readObj = null;
            try {
                readObj = objectInput.readObject();
                ret = aClazz.cast(readObj);
            } catch (final ClassCastException e) {
                final String readObjName = (readObj == null) ? "null" : readObj.getClass().getName();
                logger.error("Different type of object read [" + readObjName + "] vs. [" + aClazz.getName() + "]");
                logger.error(ExceptionUtils.getStackTrace(e));
                ret = null;
            }
        } catch (final FileNotFoundException e) {
            logger.debug("File [" + aSerializedFileName + "] not found.");
        } catch (final Exception e) {
            logger.error("An error occured during reading serialized file [" + aSerializedFileName + "]");
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            close(objectInput);
            close(fileInput);
        }
        
        return ret;
    }

    @Override
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz, T aDefaultValue) {
        final T temp = LoadFromFile(aSerializedFileName, aClazz);
        if (temp != null) {
            return temp;
        }
        
        return aDefaultValue;
    }
    
    private void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            }
            catch (final IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }
}
