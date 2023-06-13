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

public class ImportEmailRapFile {
    private static final Logger logger = LogManager.getLogger(ImportEmailRapFile.class);

    public static boolean importData(String data, Long emailConfigId) throws Exception {
        Connection connection1 = null;
        Connection connection2 = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean result = false;
        BufferedReader reader = new BufferedReader(new StringReader(data));
        boolean isRapIn = false;
        boolean isRapOut = false;
        String line;
        StringBuilder sbRapIn = new StringBuilder();
        StringBuilder sbRapOut = new StringBuilder();
        try {
            connection1 = GetConnection.connect();
            String tableImport = "";
            connection1 = GetConnection.connect();
            String sql = "select * from email.email_database_connection where type_name = 'RAP'";
            stmt = connection1.prepareStatement(sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                tableImport = rs.getString("table_import");
            } else {
                return false;
            }
            connection2 = GetConnectionToImport.connectNew("RAP");
            connection2.setAutoCommit(false);
            while ((line = reader.readLine()) != null) {
                if (line.contains("RAP IN")) {
                    isRapIn = true;
                } else if (line.contains("RAP OUT") && isRapIn) {
                    isRapIn = false;
                    isRapOut = true;
                } else if (isRapIn) {
                    if (!line.contains("RAP File") && !line.contains("Received")) {
                        sbRapIn.append(line.trim());
                        sbRapIn.append("\n");
                    }
                } else if (line.contains("END OF REPORT") && isRapOut) {
                    isRapOut = false;
                } else if (isRapOut) {
                    if (!line.contains("RAP File") && !line.contains("Received")) {
                        sbRapOut.append(line.trim());
                        sbRapOut.append("\n");
                    }
                }
            }
            BufferedReader readerIn = new BufferedReader(new StringReader(sbRapIn.toString().trim()));
            String lineIn;
            while ((lineIn = readerIn.readLine()) != null && !lineIn.contains("No RAP IN Files created")) {
                String hplmn = "";
                String vplmn = "";
                String direction = "";
                result = false;
                String[] fields = lineIn.split("\\s+");
                List<String> list = new ArrayList<String>();
                String errorDescription = StringUtils.join(" ", Arrays.copyOfRange(fields, 7, fields.length));
                list.add(0, fields[0]);
                list.add(1, fields[1]);
                list.add(2, fields[2]);
                list.add(3, fields[3]);
                list.add(4, fields[4]);
                list.add(5, fields[5]);
                list.add(6, fields[6]);
                list.add(7, errorDescription);
                direction = "I";
                hplmn = fields[1].substring(2, 7);
                vplmn = fields[1].substring(7, 12);
                result = InsertData(connection1, connection2, list, tableImport, emailConfigId, direction, hplmn, vplmn);
            }
            BufferedReader readerOut = new BufferedReader(new StringReader(sbRapOut.toString().trim()));
            String lineOut;
            while ((lineOut = readerOut.readLine()) != null && !lineOut.contains("No RAP OUT Files created")) {
                result = false;
                String hplmn = "";
                String vplmn = "";
                String direction = "";
                String[] fields = lineOut.split("\\s+");
                List<String> list = new ArrayList<String>();
                String errorDescription = StringUtils.join(" ", Arrays.copyOfRange(fields, 7, fields.length));
                list.add(0, fields[0]);
                list.add(1, fields[1]);
                list.add(2, fields[2]);
                list.add(3, fields[3]);
                list.add(4, fields[4]);
                list.add(5, fields[5]);
                list.add(6, fields[6]);
                list.add(7, errorDescription);
                direction = "O";
                hplmn = fields[1].substring(7, 12);
                vplmn = fields[1].substring(2, 7);
                result = InsertData(connection1, connection2, list, tableImport, emailConfigId, direction, hplmn, vplmn);
            }
            reader.close();
            readerIn.close();
            readerOut.close();
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

    public static boolean InsertData(Connection connection1, Connection connection2, List<String> fields, String tableImport, Long emailConfigId, String direction, String hplmn, String vplmn) throws Exception {
        boolean result = false;
        int resultInsert = 0;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String getDataImportConfig = "select * from email.email_config_detail where email_config_id = ? ";
            ps = connection1.prepareStatement(getDataImportConfig);
            ps.setLong(1, emailConfigId);
            rs = ps.executeQuery();
            List<Map<String, String>> lstAll = new ArrayList<Map<String, String>>();
            List<Map<String, String>> lstCheckExist = new ArrayList<Map<String, String>>();
            while (rs.next()) {
                String require = rs.getString("require");
                String seq = rs.getString("seq_in_file");
                String columnImport = rs.getString("column_import");
                String type = rs.getString("type");
                if (seq == null) {
                    if (rs.getString("field_name").equalsIgnoreCase("direction")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", direction);
                        map.put("data_type", type);
                        lstAll.add(map);
                        if (require.equals("1")) {
                            lstCheckExist.add(map);
                        }
                    } else if (rs.getString("field_name").equalsIgnoreCase("hpmn")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", hplmn);
                        map.put("data_type", type);
                        lstAll.add(map);
                        if (require.equals("1")) {
                            lstCheckExist.add(map);
                        }
                    } else if (rs.getString("field_name").equalsIgnoreCase("vpmn")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", vplmn);
                        map.put("data_type", type);
                        lstAll.add(map);
                        if (require.equals("1")) {
                            lstCheckExist.add(map);
                        }
                    }
                } else {
                    int seqInt = Integer.parseInt(seq) - 1;
                    String value = fields.get(seqInt);
                    if (require.equals("1")) {
                        if (value == null || value.equals("")) {
                            return false;
                        } else {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("column_import", columnImport);
                            map.put("value", value);
                            map.put("data_type", type);
                            lstCheckExist.add(map);
                        }
                    }
                    if (type.equals("text")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", rs.getString("column_import"));
                        map.put("value", value);
                        map.put("data_type", type);
                        lstAll.add(map);
                    } else if (type.equals("datetime")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", rs.getString("column_import"));
                        map.put("value", formatDatetime(value));
                        map.put("data_type", type);
                        lstAll.add(map);
                    } else if (type.equals("number")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", rs.getString("column_import"));
                        map.put("value", value);
                        map.put("data_type", type);
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
                String dataType = data.get("data_type");
                if (dataType.equals("text")) {
                    queryBuilder.append(columnName + " = '" + value + "'");
                } else if (dataType.equals("number")) {
                    queryBuilder.append(columnName + " = " + value);
                } else {
                    queryBuilder.append(columnName + " = " + "TO_DATE('" + value + "', 'yyyy-mm-dd hh24:mi:ss')");
                }

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
                    String dataType = lstAll.get(i).get("data_type");
                    String value = lstAll.get(i).get("value");
                    String columnImport = lstAll.get(i).get("column_import");
                    if (i == lstAll.size() - 1) {
                        if (dataType.equals("text")) {
                            sb.append(columnImport).append(" = '" + value + "'");
                        } else if (dataType.equals("number")) {
                            sb.append(columnImport).append(" = " + value);
                        } else {
                            sb.append(columnImport).append(" = TO_DATE('" + value + "', 'yyyy-mm-dd hh24:mi:ss')");
                        }
                    } else {
                        if (dataType.equals("text")) {
                            sb.append(columnImport).append(" = '" + value + "',");
                        } else if (dataType.equals("number")) {
                            sb.append(columnImport).append(" = " + value + ",");
                        } else {
                            sb.append(columnImport).append(" = TO_DATE('" + value + "', 'yyyy-mm-dd hh24:mi:ss')" + ",");
                        }
                    }
                }
                sb.append(" where ");
                for (int i = 0; i < lstCheckExist.size(); i++) {
                    String columnName = lstCheckExist.get(i).get("column_import");
                    String value = lstCheckExist.get(i).get("value");
                    String dataType = lstCheckExist.get(i).get("data_type");
                    if (i == lstCheckExist.size() - 1) {
                        if (dataType.equals("text")) {
                            sb.append(columnName + " = '").append(value + "'");
                        } else if (dataType.equals("number")) {
                            sb.append(columnName + " = ").append(value);
                        } else {
                            sb.append(columnName + " = ").append("TO_DATE('" + value + "','yyyy-mm-dd hh24:mi:ss')");
                        }
                    } else {
                        if (dataType.equals("text")) {
                            sb.append(columnName + " = '").append(value + "' AND ");
                        } else if (dataType.equals("number")) {
                            sb.append(columnName + " = ").append(value + " AND ");
                        } else {
                            sb.append(columnName + " = ").append("TO_DATE('" + value + "','yyyy-mm-dd hh24:mi:ss') AND ");
                        }
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
                    String dataType = (String) lstAll.get(i).get("data_type");
                    if (dataType.equals("text")) {
                        insertQuery += "'" + value + "'";
                    } else if (dataType.equals("number")) {
                        insertQuery += value;
                    } else if (dataType.equals("datetime")) {
                        insertQuery += "TO_DATE('" + value + "', 'yyyy-mm-dd hh24:mi:ss')";
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
            rs.close();
            ps.close();
        }
        return result;
    }

    public static String formatDatetime(String dateTimeString) throws Exception {
        String formattedDateTime = "";
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date date = formatter.parse(dateTimeString);
            SimpleDateFormat newFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formattedDateTime = newFormatter.format(date);
        } catch (Exception e) {
            logger.error(e);
        }
        return formattedDateTime;
    }
}
