package com.viettel.roaming.tool_import.import_data;

import com.viettel.roaming.tool_import.database.GetConnection;
import com.viettel.roaming.tool_import.database.GetConnectionToImport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

public class ImportEmailDfd {
    private static final Logger logger = LogManager.getLogger(ImportEmailDfd.class);

    public static boolean importData(String data, Long emailConfigId) throws Exception {
        Connection connection1 = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Connection connection2 = null;
        boolean result = false;
        BufferedReader reader = new BufferedReader(new StringReader(data));
        boolean isHpmn = false;
        String line = "";
        String line1 = "";
        StringBuilder sb = new StringBuilder();
        try {
            String tableImport = "";
            connection1 = GetConnection.connect();
            String sql = "select * from email.email_database_connection where type_name = 'DFD'";
            stmt = connection1.prepareStatement(sql);

            rs = stmt.executeQuery();
            if (rs.next()) {
                tableImport = rs.getString("table_import");
            } else {
                return false;
            }
            String vpmn = "";
            connection2 = GetConnectionToImport.connectNew("DFD");
            connection2.setAutoCommit(false);
            while ((line = reader.readLine()) != null) {
                if (line.contains("Customer")) {
                    String[] parts = line.split(" ");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].contains("Customer:")) {
                            vpmn = parts[i + 1];
                            break;
                        }
                    }
                }
                if (line.contains("HPMN") && line.contains("Seqnr")) {
                    isHpmn = true;
                } else if (line.contains("Total number of VPMN") && isHpmn) {
                    isHpmn = false;
                    break;
                } else if (isHpmn) {
                    sb.append(line.trim() + "\n");
                }
            }
            BufferedReader reader1 = new BufferedReader(new StringReader(sb.toString().trim()));
            while ((line1 = reader1.readLine()) != null) {
                if (!line1.matches("[\\s-]+")) {
                    List<String> list = new ArrayList<String>();
                    list.add(0, (String) line1.subSequence(0, 5));
                    list.add(1, (String) line1.subSequence(6, 11));
                    list.add(2, (String) line1.subSequence(12, 27));
                    list.add(3, (String) line1.subSequence(28, 43));
                    list.add(4, (String) line1.subSequence(44, 51));
                    list.add(5, (String) line1.subSequence(52, 55));
                    list.add(6, (String) line1.subSequence(56, 66));
                    list.add(7, (String) line1.subSequence(67, 70));
                    list.add(8, (String) line1.subSequence(71, 86));
                    list.add(9, (String) line1.subSequence(87, 102));
                    list.add(10, (String) line1.subSequence(103, 104));
                    list.add(11, (String) line1.subSequence(105, 120));
                    list.add(12, (String) line1.subSequence(121, 124));
                    list.add(13, (String) line1.subSequence(125, 128));
                    list.add(14, (String) line1.subSequence(129, 132));
                    result = InsertData(connection1, connection2, list, tableImport, emailConfigId, vpmn);
                    if (!result) {
                        break;
                    }
                }
            }
            if (result) {
                connection2.commit();
            }
            reader.close();
        } catch (Exception e) {
            logger.error("import data fail" + e);
        } finally {
            connection1.close();
            connection2.close();
        }
        return result;
    }

    public static boolean InsertData(Connection connection1, Connection connection2, List<String> fields, String tableImport, Long emailConfigId, String vpmn) throws Exception {
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
                String require = rs.getString("require");
                String seq = rs.getString("seq_in_file");
                String type = rs.getString("type");
                String columnImport = rs.getString("column_import");
                if (seq == null) {
                    String fieldName = rs.getString("field_name");
                    if (fieldName.equals("vpmn")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", vpmn);
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
                            if (type.equals("text")) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("column_import", columnImport);
                                map.put("value", value);
                                map.put("data_type", type);
                                lstCheckExist.add(map);
                            } else if (type.equals("datetime")) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("column_import", columnImport);
                                map.put("value", formatDatetime(value));
                                map.put("data_type", type);
                                lstCheckExist.add(map);
                            } else if (type.equals("number")) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("column_import", columnImport);
                                map.put("value", value);
                                map.put("data_type", type);
                                lstCheckExist.add(map);
                            }
                        }
                    }
                    if (type.equals("text")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", value);
                        map.put("data_type", type);
                        lstAll.add(map);
                    } else if (type.equals("datetime")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
                        map.put("value", formatDatetime(value));
                        map.put("data_type", type);
                        lstAll.add(map);
                    } else if (type.equals("number")) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("column_import", columnImport);
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
            System.out.println(e);
        } finally {
            rs.close();
            ps.close();
        }
        return result;
    }

    public static String formatDatetime(String dateTimeString) throws Exception {
        String formattedDateTime = "";
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy HH:mm");
            Date date = formatter.parse(dateTimeString);
            SimpleDateFormat newFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formattedDateTime = newFormatter.format(date);
        } catch (Exception e) {
            logger.error(e);
        }
        return formattedDateTime;
    }
}
