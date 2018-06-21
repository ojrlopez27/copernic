package de.nec.nle.siafu.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

/**
 * Created by oscarr on 5/3/18.
 */
public class MapSimulationData extends DirectorySimulationData{
  
    public MapSimulationData(final File path, HashMap<String, Class> map) {
        super(path);
        this.map = map;
    }

    public MapSimulationData(final File path) {
        super(path);
    }

    @Override
    protected InputStream getFile(String path) {
        return super.getFile(path);
    }

    @Override
    protected HashMap<String, InputStream> getFilesByPath(String path) {
        return super.getFilesByPath(path);
    }

    @Override
    protected ArrayList<String> getFileNamesByPath(String path) {
        return super.getFileNamesByPath(path);
    }
}
