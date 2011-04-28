package web;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import exception.DaoException;

public class LookupQuery {

	final public static String userQuery = "select user_id, username, full_name, nik, status, failed_logins, password_expiry,email_address,department,division,address,user.group, ROW_NUMBER() OVER(ORDER BY user_id DESC) AS ROWNUMBER  from user";
	final public static String userQueryTable = "user";
	final public static String userQueryCount = "select count(1) from user";
	final public static String userAuditQuery = "select user_audit.user,ip_address,date_time,action, ROW_NUMBER() OVER(ORDER BY user DESC) AS ROWNUMBER  from user_audit";
	final public static String userAuditQueryTable = "user_audit";
	final public static String userAuditQueryCount = "select count(1) from user_audit";
	final public static String kodeBarangQuery = "select \"VISITEK-117\".TBMASBAR.kode_bar,\"VISITEK-117\".TBMASBAR.nama_bar, ROW_NUMBER() OVER(ORDER BY \"VISITEK-117\".TBMASBAR.kode_bar DESC) AS ROWNUMBER from \"VISITEK-117\".TBMASBAR";
	final public static String kodeBarangQueryTable = "\"VISITEK-117\".TBMASBAR";
	final public static String kodeBarangQueryCount = "select count(1) from \"VISITEK-117\".TBMASBAR";
	final public static String kodeSalesmanQuery = "select \"VISITEK-117\".TBMASBAR.nama_bar,\"VISITEK-117\".TBMASBAR.kode_bar, ROW_NUMBER() OVER(ORDER BY \"VISITEK-117\".TBMASBAR.kode_bar DESC) AS ROWNUMBER from \"VISITEK-117\".TBMASBAR";
	final public static String kodeSalesmanQueryTable = "\"VISITEK-117\".TBMASBAR";
	final public static String kodeSalesmanQueryCount = "select count(1) from \"VISITEK-117\".TBMASBAR";
	final public static String kodeSOQuery = "select \"VISITEK-117\".TBSO_SMS.no_so_sms,\"VISITEK-117\".TBSO_SMS.tgl_so_sms,\"VISITEK-117\".TBSO_SMS.no_po,\"VISITEK-117\".TBSO_SMS.tgl_po, ROW_NUMBER() OVER(ORDER BY \"VISITEK-117\".TBSO_SMS.tgl_so_sms DESC) AS ROWNUMBER from \"VISITEK-117\".TBSO_SMS where \"VISITEK-117\".TBSO_SMS.F_SOBATAL=0 and \"VISITEK-117\".TBSO_SMS.F_APPCAB=0 and \"VISITEK-117\".TBSO_SMS.F_APPPROTEK=0";
	final public static String kodeSOQueryTable = "\"VISITEK-117\".TBSO_SMS";
	final public static String kodeSOQueryCount = "select count(1) from \"VISITEK-117\".TBSO_SMS where \"VISITEK-117\".TBSO_SMS.F_SOBATAL=0 and \"VISITEK-117\".TBSO_SMS.F_APPCAB=0 and \"VISITEK-117\".TBSO_SMS.F_APPPROTEK=0";

	public static int countNumberLike(Connection conn, String filter,
			String field, String STMT_QUERY) throws DaoException {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			Field f = LookupQuery.class.getDeclaredField(STMT_QUERY + "Count");
			String query = (String) f.get(LookupQuery.class);
			f = LookupQuery.class.getDeclaredField(STMT_QUERY + "Table");
			String table = (String) f.get(LookupQuery.class);
			if (query.indexOf("where") > 0)
				stmt = conn.prepareStatement(query + " and " + table + "."
						+ filter + " like '%" + field + "%'");
			else
				stmt = conn.prepareStatement(query + " where " + table + "."
						+ filter + " like '%" + field + "%'");
			rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new DaoException("Error retrieving count of users");
			}
		} catch (Exception ex) {
			throw new DaoException(ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static List<ArrayList<String>> listlike(Connection conn,
			String filter, String field, int start, int rows,
			String STMT_QUERY, String showedField) throws DaoException {

		List<ArrayList<String>> datas = new ArrayList<ArrayList<String>>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			Field f = LookupQuery.class.getDeclaredField(STMT_QUERY);
			String query = (String) f.get(LookupQuery.class);
			f = LookupQuery.class.getDeclaredField(STMT_QUERY + "Table");
			String table = (String) f.get(LookupQuery.class);
			if ((start == -1) || (rows == -1)) {
				stmt = conn.prepareStatement(query);
			} else {
				if (query.indexOf("where") > 0)
					stmt = conn.prepareStatement("select * from (" + query
							+ " and " + table + "." + filter + " like '%"
							+ field
							+ "%') as tbl where tbl.ROWNUMBER between ? and ?");
				else
					stmt = conn.prepareStatement("select * from (" + query
							+ " where " + table + "." + filter + " like '%"
							+ field
							+ "%') as tbl where tbl.ROWNUMBER between ? and ?");
				stmt.setInt(1, start);
				stmt.setInt(2, start + rows);
			}
			rs = stmt.executeQuery();

			while (rs.next()) {
				ArrayList<String> data = new ArrayList<String>();
				for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
					if (showedField.toUpperCase().contains(
							rs.getMetaData().getColumnName(i)))
						data.add(rs.getString(i));
				}
				datas.add(data);
			}

			return datas;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new DaoException(ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}