package com.viettel.roaming.tool_import.import_data;

import com.viettel.roaming.tool_import.database.GetConnection;
import com.viettel.roaming.tool_import.database.GetConnectionToImport;
import com.viettel.roaming.tool_import.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

public class ImportEmailHur2 {
    private static final Logger logger = LogManager.getLogger(ImportEmailHur2.class);

    public static boolean importData(String data, Long emailConfigId) throws Exception {
        Connection connection1 = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection connection2 = null;
        boolean result = false;
        BufferedReader reader = new BufferedReader(new StringReader(data));
        String line;
        try {
            String tableImport = "";
            connection1 = GetConnection.connect();
            String sql = "select * from email.email_database_connection where type_name = 'HUR2'";
            stmt = connection1.prepareStatement(sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                tableImport = rs.getString("table_import");
            } else {
                return false;
            }
            connection2 = GetConnectionToImport.connectNew("HUR2");
            connection2.setAutoCommit(false);
            String hplmn = "";
            String vplmn = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("H")) {
                    List<String> fields = Arrays.asList(line.split(","));
                    hplmn = fields.get(1);
                    vplmn = fields.get(2);
                }
                if (line.startsWith("C")) {
                    List<String> fields = Arrays.asList(line.split(","));
                    result = InsertData(connection1, connection2, fields, tableImport, emailConfigId, hplmn, vplmn);
                    if (!result) {
                        break;
                    }
                }
            }
            if (result) {
                connection2.commit();
            }

        } catch (Exception e) {
            logger.error("import data fail" + e);
        } finally {
            connection1.close();
            connection2.close();
        }
        return result;
    }


    public static boolean InsertData(Connection connection1, Connection connection2, List<String> fields, String tableImport, Long emailConfigId, String hplmn, String vplmn) throws Exception {
        boolean result = false;
        int resultInsert = 0;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Map<String, String>> lstAll = new ArrayList<Map<String, String>>();
        List<Map<String, String>> lstCheckExist = new ArrayList<Map<String, String>>();
        try {
            String getDataImportConfig = "select * from email.email_config_detail where email_config_id = ? ";
            ps = connection1.prepareStatement(getDataImportConfig);
            ps.setLong(1, emailConfigId);
            rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                String columnImport = rs.getString("column_import");
                String require = rs.getString("require");
                if (type.equals("text")) {
                    String seq = rs.getString("seq_in_file");
                    if (seq == null) {
                        if (rs.getString("field_name").equalsIgnoreCase("hpmn")) {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("column_import", columnImport);
                            map.put("value", hplmn);
                            lstAll.add(map);
                            if(require.equals("1")){
                                lstCheckExist.add(map);
                            }
                        } else if (rs.getString("field_name").equalsIgnoreCase("vpmn")) {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("column_import", columnImport);
                            map.put("value", vplmn);
                            lstAll.add(map);
                            if(require.equals("1")){
                                lstCheckExist.add(map);
                            }
                        }
                    } else {
                        int seqInt = Integer.parseInt(seq) - 1;
                        if (require.equals("1")) {
                            if (fields.get(seqInt) == null || fields.get(seqInt).equals("")) {
                                return false;
                            } else {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("column_import", columnImport);
                                map.put("value", fields.get(seqInt));
                                lstCheckExist.add(map);
                            }
                        }
                        if (!StringUtils.isBlank(fields.get(seqInt))) {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("column_import", columnImport);
                            map.put("value", fields.get(seqInt));
                            lstAll.add(map);
                        }
                    }
                } else if (type.equals("datetime")) {
                    String seqInFile = rs.getString("seq_in_file");
                    String[] seqInFileLst = seqInFile.split(";");
                    String dateTime = "";
                    for (String seq : seqInFileLst) {
                        int seqInt = Integer.parseInt(seq) - 1;
                        if (!StringUtils.isBlank(fields.get(seqInt))) {
                            dateTime += fields.get(seqInt);
                        }
                    }
                    if (require.equals("1")) {
                        if (dateTime.equals("")) {
                            return false;
                        } else {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("column_import", columnImport);
                            if (dateTime.length() == 8) {
                                map.put("value", formatDatetime2(dateTime));
                            }
                            if (dateTime.length() > 8) {
                                map.put("value", formatDatetime(dateTime));
                            }
                            lstCheckExist.add(map);
                        }
                    }
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("column_import", columnImport);
                    if (dateTime.length() == 8) {
                        map.put("value", formatDatetime2(dateTime));
                    }
                    if (dateTime.length() > 8) {
                        map.put("value", formatDatetime(dateTime));
                    }
                    lstAll.add(map);
                } else if (type.equals("number")) {
                    String seq = rs.getString("seq_in_file");
                    int seqInt = Integer.parseInt(seq) - 1;
                    if (require.equals("1")) {
                        if (fields.get(seqInt) == null || fields.get(seqInt).equals("")) {
                            return false;
                        } else {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("column_import", columnImport);
                            map.put("value", fields.get(seqInt));
                            lstCheckExist.add(map);
                        }
                    }
                    if (!StringUtils.isBlank(fields.get(seqInt))) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", fields.get(seqInt));
                        lstAll.add(map);
                    }
                }
            }
            StringBuilder queryBuilder = new StringBuilder("SELECT 1 FROM ");
            queryBuilder.append(tableImport + " WHERE ");
            for (int i = 0; i < lstCheckExist.size(); i++) {
                Map<String, String> data = lstCheckExist.get(i);
                String columnName = data.get("column_import");
                String value = data.get("value");
                queryBuilder.append(columnName + " = '" + value + "'");

                if (i < lstCheckExist.size() - 1) {
                    queryBuilder.append(" AND ");
                }
            }
            ps = connection2.prepareStatement(queryBuilder.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
                StringBuilder sb = new StringBuilder(" update ");
                sb.append(tableImport).append(" set ");
                for (int i = 0; i < lstAll.size(); i++) {
                    if (i == lstAll.size() - 1) {
                        sb.append(lstAll.get(i).get("column_import")).append(" = '" + lstAll.get(i).get("value") + "'");
                    } else {
                        sb.append(lstAll.get(i).get("column_import")).append(" = '" + lstAll.get(i).get("value") + "',");
                    }
                }
                sb.append(" where ");
                for (int i = 0; i < lstCheckExist.size(); i++) {
                    if (i == lstCheckExist.size() - 1){
                        sb.append(lstCheckExist.get(i).get("column_import") + " = '").append(lstCheckExist.get(i).get("value")+"'");
                    } else {
                        sb.append(lstCheckExist.get(i).get("column_import") + " = '").append(lstCheckExist.get(i).get("value")+"' AND ");
                    }
                }
                ps = connection2.prepareStatement(sb.toString());
            } else {
                String insertQuery = "INSERT INTO " + tableImport + " (";
                for (int i = 0; i < lstAll.size(); i++) {
                    String column = (String) lstAll.get(i).get("column_import");
                    insertQuery += column;
                    if (i < lstAll.size() - 1) {
                        insertQuery += ",";
                    }
                }
                insertQuery += ") VALUES (";
                for (int i = 0; i < lstAll.size(); i++) {
                    String value = (String) lstAll.get(i).get("value");
                    insertQuery += "'" + value + "'";
                    if (i < lstAll.size() - 1) {
                        insertQuery += ",";
                    }
                }
                insertQuery += ")";
                ps = connection2.prepareStatement(insertQuery);
            }
            resultInsert = ps.executeUpdate();
            if (resultInsert == 1) {
                result = true;
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            ps.close();
            rs.close();
        }
        return result;
    }

    public static String formatDatetime(String dateTimeString) throws Exception {
        String formattedDateTime = "";
        try {
            SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
            SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dateTime = inputFormatter.parse(dateTimeString);
            formattedDateTime = outputFormatter.format(dateTime);
        } catch (Exception e) {
            logger.error(e);
        }
        return formattedDateTime;
    }

    public static String formatDatetime2(String dateTimeString) throws Exception {
        String formattedDateTime = "";
        try {
            SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dateTime = inputFormatter.parse(dateTimeString);
            formattedDateTime = outputFormatter.format(dateTime);
        } catch (Exception e) {
            logger.error(e);
        }
        return formattedDateTime;
    }
}
