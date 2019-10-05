package ayyeka.assignment.ngnix_access_logs_parser.service;

import ayyeka.assignment.ngnix_access_logs_parser.dao.DataBaseApiInterface;
import ayyeka.assignment.ngnix_access_logs_parser.model.NginxLogfile;
import ayyeka.assignment.ngnix_access_logs_parser.model.NginxLogfileRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;


import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Parses an nginx file and saves its data into the database.
 * */

@Service
public class NginxLogfileParser {
    /**
     * opens up the logfile corresponding to 'key',
     * creates and saves a new instance of 'NginxLogfile',
     * and then parses & persists each line into a corresponding 'NginxLogfileRow'
     *
     * @param key*/
    private void parseLogfile(String key) {
        System.out.println(key + "...");
        String line;
        BufferedReader br = logfileResolver.resolveFor(key);
        NginxLogfile nginxLogfile = new NginxLogfile(key);
        Long logFileId = db.insertLogfile(nginxLogfile);
        nginxLogfile.setId(logFileId);
        try{
            while ((line = br.readLine()) != null) {
                Map<String, String> parsedTokenMap = detokenizer.parse(line);
                final NginxLogfileRow row = NginxLogfileRow.fromMap(parsedTokenMap, nginxLogfile);
                db.insertRequest(row.getRequest());
                db.insertRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private Executor executor = Executors.newSingleThreadExecutor();
    public void enqueueLogfile(String key){
        executor.execute(() -> parseLogfile(key));
    }

    @Autowired
    private DataBaseApiInterface db;
    @Autowired
    private LogfileResolver logfileResolver;
    @Value("${log-format}")
    private String logFormat;
    private Detokenizer detokenizer;
    @PostConstruct
    private void initDetokenizer(){
        detokenizer = Detokenizer.of(logFormat);
    }
}
