package servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import web.User;
import web.UserSession;
import database.Connector;
import exception.DaoException;
import exception.UserNotFoundException;

/**
 * Servlet implementation class StockOrderUpdate
 */
public class StockOrderUpdate extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = Logger.getLogger(StockOrderUpdate.class);
	private final String tableQuery = "SELECT ROW_NUMBER() OVER(ORDER BY TGL_SO_SMS DESC) as \"No\", TGL_SO_SMS as \"Tgl SO\",NO_SO_SMS as \"No SO\",TGL_PO as \"Tgl PO\",NO_PO as \"No PO\",case when TYPE_BAYAR = 1.0 then 'kredit' else 'tunai' end as \"tipe bayar\",case when F_SOBATAL = 1 then 'Batal' else case when F_APPCAB = 1 then 'Setuju' else case when F_APPPROTEK = 1 then 'Proteksi' else 'Menunggu' end end end as \"Status\",KET_SO as \"Keterangan\", ROW_NUMBER() OVER(ORDER BY TGL_SO_SMS DESC) AS ROWNUMBER FROM \"DB2ADMIN\".TBSO_SMS where (NO_SO_SMS=? or TGL_SO_SMS between ? and ?) and F_SOBATAL = 0 and F_APPCAB = 0 and F_APPPROTEK = 0 and KODE_CAB=? and KODE_CUST=?";
	private final String tableQueryCount = "SELECT count(1) FROM \"DB2ADMIN\".TBSO_SMS where (NO_SO_SMS=? or TGL_SO_SMS between ? and ?) and F_SOBATAL = 0 and F_APPCAB = 0 and F_APPPROTEK = 0  and KODE_CAB=? and KODE_CUST=?";
	private final String editQuery = "SELECT A.TYPE_BAYAR,A.TGL_SO_SMS,A.TGL_PO,A.NO_SO_SMS,A.NO_PO,A.KODE_TYPESO,A.KODE_TYPEDO,A.KODE_TRN,A.KODE_PEG,A.KODE_CUST,A.KODE_CAB,A.KET_SO FROM \"DB2ADMIN\".TBSO_SMS as A where A.NO_SO_SMS=?";
	private final String editQueryDetail = "SELECT B.NO_URUT,B.KODE_BAR,A.nama_bar,A.satuan_bar, A.kemasan_bar,B.QTY_SO FROM \"DB2ADMIN\".TBMASBAR as A,\"DB2ADMIN\".TBDTSO_SMS as B where A.kode_bar=B.kode_bar and B.NO_SO_SMS=?";
	private final String comboTypeSOQuery = "select NAMA_TYPESO,KODE_TYPESO from \"DB2ADMIN\".tbmastypeso where KODE_TYPESO=2 or KODE_TYPESO=4";
	private final String comboJenisTransaksi = "select NAMA_TRN,KODE_TRN from \"DB2ADMIN\".tbmastrn where KODE_TRN=1 or KODE_TRN=3";
	private final String comboTipeTransaksi = "select NAMA_TYPEDO,KODE_TYPEDO from \"DB2ADMIN\".tbmastypedo where KODE_TYPEDO=1 or KODE_TYPEDO=2";
	private final String comboJenisObat = "select NAMA_JNSOBAT,KODE_JNSOBAT from \"DB2ADMIN\".tbmasjnsobat where KODE_JNSOBAT = 1 AND F_AKTIF =1";
	private final String simpanOrder = "call SPIUSO_SMS(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private final String hapusOrder = "call SPDELSO_SMS(?,?,?)";
	private final String simpanOrderDetail = "call SPIUDTSO_SMS(?,?,?,?,?,?)";
	private final String hapusOrderDetail = "call SPDELDTSO_SMS(?,?)";
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StockOrderUpdate() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String page = (String) request.getParameter("page");
		String tanggaSOAwal = request.getParameter("tanggal_so_awal");
		String tanggaSOAkhir = request.getParameter("tanggal_so_akhir");
		String nomorSO = request.getParameter("nomor_so");
		String nomorSOEdit = request.getParameter("no_so");
		if (page != null && nomorSOEdit == null) {

			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				int total_pages = -1;
				Connection conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(tableQueryCount);
				pstmt.setString(1, nomorSO);
				pstmt.setString(2, (tanggaSOAwal == null || tanggaSOAwal
						.equals("")) ? sdf.format(new Date()) : tanggaSOAwal);
				pstmt.setString(3, (tanggaSOAkhir == null || tanggaSOAkhir
						.equals("")) ? sdf.format(new Date()) : tanggaSOAkhir);
				pstmt.setString(4, UserSession.Factory.getUserSession(request).getUser().getKodeCabang());
				pstmt.setString(5, UserSession.Factory.getUserSession(request).getUser().getKodeCustomer());
				rs = pstmt.executeQuery();
				if (rs.next())
					total_pages = rs.getInt(1);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement("select * from (" + tableQuery
						+ ") as tbl where tbl.ROWNUMBER between ? and ?");
				pstmt.setString(1, nomorSO);
				pstmt.setString(2, (tanggaSOAwal == null || tanggaSOAwal
						.equals("")) ? sdf.format(new Date()) : tanggaSOAwal);
				pstmt.setString(3, (tanggaSOAkhir == null || tanggaSOAkhir
						.equals("")) ? sdf.format(new Date()) : tanggaSOAkhir);
				pstmt.setString(4, UserSession.Factory.getUserSession(request).getUser().getKodeCabang());
				pstmt.setString(5, UserSession.Factory.getUserSession(request).getUser().getKodeCustomer());
				pstmt.setInt(6, 10 * (Integer.parseInt(page) - 1));
				pstmt.setInt(7, (10 * (Integer.parseInt(page) - 1)) + 10);
				rs = pstmt.executeQuery();
				ArrayList<String> tableColumn = new ArrayList<String>();
				ArrayList<ArrayList<String>> tableData = new ArrayList<ArrayList<String>>();
				int totalColumn = rs.getMetaData().getColumnCount();
				for (int i = 1; i < totalColumn; i++)
					tableColumn.add(rs.getMetaData().getColumnName(i));
				request.setAttribute("tableColumn", tableColumn);
				while (rs.next()) {
					ArrayList<String> rowData = new ArrayList<String>();
					for (int i = 1; i < totalColumn; i++) {
						rowData.add(rs.getString(i));
					}
					tableData.add(rowData);
				}
				request.setAttribute("tableData", tableData);
				request.setAttribute(
						"total_pages",
						Integer.toString((total_pages / 10)
								+ ((total_pages % 10) > 0 ? 1 : 0)));
				request.setAttribute("pages", page);
			} catch (DaoException ex) {
				ex.printStackTrace();
				logger.error(ex);
			} catch (NamingException ex) {
				ex.printStackTrace();
				logger.error(ex);
			} catch (SQLException ex) {
				logger.error(ex);
				ex.printStackTrace();
			} finally {
				try {
					if (rs != null)
						rs.close();
					if (pstmt != null)
						pstmt.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else if (nomorSOEdit != null) {
			PreparedStatement pstmt = null;
			ResultSet rs = null;

			try {
				Connection conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(editQuery);
				pstmt.setString(1, nomorSOEdit);
				rs = pstmt.executeQuery();
				HashMap<String, String> data = new HashMap<String, String>();
				int totalColumn = rs.getMetaData().getColumnCount();
				if (rs.next())
					for (int i = 1; i <= totalColumn; i++)
						data.put(rs.getMetaData().getColumnName(i),
								rs.getString(i));
				request.setAttribute("data", data);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(editQueryDetail);
				pstmt.setString(1, nomorSOEdit);
				rs = pstmt.executeQuery();
				ArrayList<ArrayList<String>> dataDetail = new ArrayList<ArrayList<String>>();
				totalColumn = rs.getMetaData().getColumnCount();
				while (rs.next()) {
					ArrayList<String> singleData = new ArrayList<String>();
					for (int i = 1; i <= totalColumn; i++)
						singleData.add(rs.getString(i));
					dataDetail.add(singleData);
				}
				request.setAttribute("dataDetail", dataDetail);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(comboTypeSOQuery);
				rs = pstmt.executeQuery();
				ArrayList<String> comboTypeSOQuery = new ArrayList<String>();
				while (rs.next()) {
					comboTypeSOQuery.add(rs.getString(1) + ","
							+ rs.getString(2));
				}
				request.setAttribute("comboTypeSO", comboTypeSOQuery);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(comboJenisTransaksi);
				rs = pstmt.executeQuery();
				ArrayList<String> comboJenisTransaksi = new ArrayList<String>();
				while (rs.next()) {
					comboJenisTransaksi.add(rs.getString(1) + ","
							+ rs.getString(2));
				}
				request.setAttribute("comboJenisTransaksi", comboJenisTransaksi);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(comboJenisObat);
				rs = pstmt.executeQuery();
				ArrayList<String> comboJenisObat = new ArrayList<String>();
				while (rs.next()) {
					comboJenisObat.add(rs.getString(1) + "," + rs.getString(2));
				}
				request.setAttribute("comboJenisObat", comboJenisObat);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(comboTipeTransaksi);
				rs = pstmt.executeQuery();
				ArrayList<String> comboTipeTransaksi = new ArrayList<String>();
				while (rs.next()) {
					comboTipeTransaksi.add(rs.getString(1) + ","
							+ rs.getString(2));
				}
				request.setAttribute("comboTipeTransaksi", comboTipeTransaksi);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DaoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			request.getRequestDispatcher("/pages/so/stock-order-modify.jsp")
					.forward(request, response);
		}

		request.getRequestDispatcher("/pages/so/stock-order-update.jsp")
				.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("Action") != null) {

			String action = request.getParameter("Action");
			if (action.equalsIgnoreCase("Search")) {
				String tanggaSOAwal = request.getParameter("tanggal_so_awal");
				String tanggaSOAkhir = request.getParameter("tanggal_so_akhir");
				String nomorSO = request.getParameter("nomor_so");
				PreparedStatement pstmt = null;
				ResultSet rs = null;
				try {
					int total_pages = -1;
					Connection conn = Connector.getInstance()
							.getConnectionAdmin();
					pstmt = conn.prepareStatement(tableQueryCount);
					pstmt.setString(1, nomorSO);
					pstmt.setString(
							2,
							(tanggaSOAwal == null || tanggaSOAwal.equals("")) ? sdf.format(new Date(
									System.currentTimeMillis() + 86400000))
									: tanggaSOAwal);
					pstmt.setString(
							3,
							(tanggaSOAkhir == null || tanggaSOAkhir.equals("")) ? sdf.format(new Date(
									System.currentTimeMillis() + 86400000))
									: tanggaSOAkhir);
					pstmt.setString(4, UserSession.Factory.getUserSession(request).getUser().getKodeCabang());
					pstmt.setString(5, UserSession.Factory.getUserSession(request).getUser().getKodeCustomer());
					rs = pstmt.executeQuery();
					if (rs.next())
						total_pages = rs.getInt(1);
					rs.close();
					pstmt.close();

					conn = Connector.getInstance().getConnectionAdmin();
					pstmt = conn.prepareStatement("select * from ("
							+ tableQuery
							+ ") as tbl where tbl.ROWNUMBER between ? and ?");
					pstmt.setString(1, nomorSO);
					pstmt.setString(
							2,
							(tanggaSOAwal == null || tanggaSOAwal.equals("")) ? sdf.format(new Date(
									System.currentTimeMillis() + 86400000))
									: tanggaSOAwal);
					pstmt.setString(
							3,
							(tanggaSOAkhir == null || tanggaSOAkhir.equals("")) ? sdf.format(new Date(
									System.currentTimeMillis() + 86400000))
									: tanggaSOAkhir);
					pstmt.setString(4, UserSession.Factory.getUserSession(request).getUser().getKodeCabang());
					pstmt.setString(5, UserSession.Factory.getUserSession(request).getUser().getKodeCustomer());
					pstmt.setInt(6, 0);
					pstmt.setInt(7, 10);
					rs = pstmt.executeQuery();

					ArrayList<String> tableColumn = new ArrayList<String>();
					ArrayList<ArrayList<String>> tableData = new ArrayList<ArrayList<String>>();
					int totalColumn = rs.getMetaData().getColumnCount();
					for (int i = 1; i < totalColumn; i++)
						tableColumn.add(rs.getMetaData().getColumnName(i));
					request.setAttribute("tableColumn", tableColumn);
					while (rs.next()) {
						ArrayList<String> rowData = new ArrayList<String>();
						for (int i = 1; i < totalColumn; i++) {
							rowData.add(rs.getString(i));
						}
						tableData.add(rowData);
					}
					request.setAttribute("tableData", tableData);
					request.setAttribute(
							"total_pages",
							Integer.toString((total_pages / 10)
									+ ((total_pages % 10) > 0 ? 1 : 0)));
				} catch (DaoException ex) {
					ex.printStackTrace();
					// logger.error(ex);
				} catch (NamingException ex) {
					ex.printStackTrace();
					// logger.error(ex);
				} catch (SQLException ex) {
					ex.printStackTrace();
					// logger.error(ex);
				} catch (Exception e) {
					e.printStackTrace();
				}
				request.getRequestDispatcher(
						"/pages/so/stock-order-update.jsp?&tanggal_so_awal="
								+ tanggaSOAwal + "&tanggal_so_akhir="
								+ tanggaSOAkhir + "&nomor_so=" + nomorSO)
						.forward(request, response);
			} else if (action.equalsIgnoreCase("Hapus")) {
				if (request.getParameterValues("deleted") != null) {
					String[] delete = (String[]) request
							.getParameterValues("deleted");
					PreparedStatement pstmt = null;
					for (@SuppressWarnings("unused")
					String str : delete)
						try {

							int i = 0;
							while (delete.length > i) {
								Connection conn = Connector.getInstance()
										.getConnection();
								pstmt = conn
										.prepareStatement(User.Factory.STMT_INSERT_USER_AUDIT);
								UserSession userSession = UserSession.Factory
										.getUserSession(request);
								pstmt.setString(1, delete[0]);
								pstmt.setString(2, request.getRemoteAddr());
								pstmt.setString(3,
										"Delete order with NO SO SMS "
												+ delete[i].split(";")[1]
												+ " by "
												+ userSession.getUser()
														.getUsername());
								pstmt.executeUpdate();

								conn = Connector.getInstance()
										.getConnectionAdmin();
								pstmt = conn.prepareStatement(hapusOrder);
								pstmt.setString(1, userSession.getUser()
										.getKodeCabang());
								pstmt.setString(2, delete[i].split(";")[1]);
								pstmt.setString(3, delete[i].split(";")[2]);
								pstmt.executeUpdate();
								pstmt.close();

								conn = Connector.getInstance()
										.getConnectionAdmin();
								pstmt = conn.prepareStatement(hapusOrderDetail);
								pstmt.setString(1, userSession.getUser()
										.getKodeCabang());
								pstmt.setString(2, delete[i].split(";")[1]);
								pstmt.executeUpdate();
								i++;
							}
							pstmt.close();

						} catch (DaoException ex) {
							logger.error(ex);
						} catch (NamingException ex) {
							logger.error(ex);
						} catch (SQLException ex) {
							logger.error(ex);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							try {
								if (pstmt != null)
									pstmt.close();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					request.setAttribute("deletedData", delete);
					request.setAttribute("message",
							"Stock Order has been deleted successfully");
					request.getRequestDispatcher(
							"/pages/so/stock-order-update-success.jsp")
							.forward(request, response);
				} else {
					PreparedStatement pstmt = null;
					String noSO = request.getParameter("NO_SO_SMS");
					String kodeCabang = request.getParameter("KODE_CAB");
					String noPO = "PO." + request.getParameter("po");
					try {
						Connection conn = Connector.getInstance()
								.getConnection();
						pstmt = conn
								.prepareStatement(User.Factory.STMT_INSERT_USER_AUDIT);
						UserSession userSession = UserSession.Factory
								.getUserSession(request);
						pstmt.setString(1, noSO);
						pstmt.setString(2, request.getRemoteAddr());
						pstmt.setString(3, "Delete order with NO SO SMS "
								+ noSO + " by "
								+ userSession.getUser().getUsername());
						pstmt.executeUpdate();

						conn = Connector.getInstance().getConnectionAdmin();
						pstmt = conn.prepareStatement(hapusOrder);
						pstmt.setString(1, kodeCabang);
						pstmt.setString(2, noSO);
						pstmt.setString(3, noPO);
						pstmt.executeUpdate();
						pstmt.close();

						conn = Connector.getInstance().getConnectionAdmin();
						pstmt = conn.prepareStatement(hapusOrderDetail);
						pstmt.setString(1, kodeCabang);
						pstmt.setString(2, noSO);
						pstmt.executeUpdate();

					} catch (DaoException ex) {
						logger.error(ex);
					} catch (NamingException ex) {
						logger.error(ex);
					} catch (SQLException ex) {
						logger.error(ex);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (pstmt != null)
								pstmt.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					request.setAttribute("no_so", noSO);
					request.setAttribute("tanggal_so",
							request.getParameter("tanggal_so"));
					request.setAttribute("no_po", noPO);
					request.setAttribute("tanggal_po",
							request.getParameter("tanggal_po"));
					request.setAttribute("kode_barang",
							request.getParameter("kode_barang"));
					request.setAttribute("quantity",
							request.getParameter("quantity_so"));
					request.setAttribute("tipe_bayar",
							request.getParameter("tipe_bayar"));
					request.setAttribute("catatan",
							request.getParameter("catatan"));
					request.setAttribute("message",
							"Stock Order has been deleted successfully");
					request.getRequestDispatcher(
							"/pages/so/stock-order-modify-success.jsp")
							.forward(request, response);
				}
			} else if (action.equalsIgnoreCase("Koreksi")) {
				String[] delete = null;
				if (request.getParameterValues("deleted") != null) {
					delete = (String[]) request.getParameterValues("deleted");
				}
				response.sendRedirect(request.getContextPath()
						+ "/pages/stock-order-update?no_so="
						+ delete[0].split(";")[1]);
			} else if (action.equalsIgnoreCase("Baru")) {
				StockOrder so = new StockOrder();
				so.doGet(request, response);
			} else if (action.equalsIgnoreCase("Keluar")) {
				request.getRequestDispatcher("/").forward(request, response);
			} else if (action.equalsIgnoreCase("Batal")) {
				response.sendRedirect(request.getContextPath()
						+ "/pages/stock-order-update?page=1&tanggal_so_awal="
						+ sdf.format(new Date()) + "&tanggal_so_akhir="
						+ sdf.format(new Date()) + "&nomor_so=");
			} else if (action.equalsIgnoreCase("Simpan")) {
				String noSO = request.getParameter("NO_SO_SMS");
				String kodeCabang = request.getParameter("KODE_CAB");
				String kodeCustomer = request.getParameter("KODE_CUST");
				String tanggalSo = request.getParameter("tanggal_so");
				String noPo = request.getParameter("po");
				String tanggalPo = request.getParameter("tanggal_po");
				String[] kodeBarang = request.getParameterValues("kodebarang");				
				String kodeTransaksi = request.getParameter("jenis_transaksi");
				String[] qty = request.getParameterValues("qty");				
				String tipeBayar = request.getParameter("tipe_bayar");
				String tipeDO = request.getParameter("tipe_transaksi");
				String catatan = request.getParameter("catatan");
				String tipeSO = request.getParameter("type_so");
				String konsinyasi = tipeSO.equals("4") ? "1" : "0";				

				PreparedStatement pstmt = null;
				try {
					Connection conn = Connector.getInstance()
							.getConnectionAdmin();
					pstmt = conn.prepareStatement(simpanOrder);
					pstmt.setString(1, kodeCabang);
					pstmt.setString(2, kodeTransaksi);
					pstmt.setString(3, kodeCustomer);
					pstmt.setString(4, "9999");
					pstmt.setString(5, tipeSO);
					pstmt.setString(6, tipeDO);
					pstmt.setString(7, tanggalSo);
					pstmt.setString(8, noSO);
					pstmt.setString(9, tanggalPo);
					pstmt.setString(10, noPo);
					pstmt.setString(11, tipeBayar.equals("tunai") ? "0" : "1");
					pstmt.setString(12, "0");
					pstmt.setString(13, konsinyasi);
					pstmt.setString(14, "0");
					pstmt.setString(15, "0");
					pstmt.setString(16, catatan);
					pstmt.setInt(17, 1);
					pstmt.executeUpdate();
					pstmt.close();
					
					conn = Connector.getInstance().getConnectionAdmin();
					pstmt = conn.prepareStatement(hapusOrderDetail);
					pstmt.setString(1,
							UserSession.Factory.getUserSession(request)
									.getUser().getKodeCabang());
					pstmt.setString(2, noSO);
					pstmt.executeUpdate();
					pstmt.close();
					
					for (int cnt = 0; cnt < qty.length; cnt++) {												
						conn = Connector.getInstance().getConnectionAdmin();
						pstmt = conn.prepareStatement(simpanOrderDetail);
						pstmt.setString(1, UserSession.Factory.getUserSession(request)
								.getUser().getKodeCabang());
						pstmt.setString(2, noSO);
						pstmt.setInt(3, cnt);
						pstmt.setString(4, kodeBarang[cnt]);
						pstmt.setString(5, qty[cnt]);
						pstmt.setString(6, qty[cnt]);
						
						pstmt.executeUpdate();
						pstmt.close();
					}					

				} catch (DaoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NamingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						if (pstmt != null)
							pstmt.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				request.setAttribute("no_so", noSO);
				request.setAttribute("tanggal_so", tanggalSo);
				request.setAttribute("no_po", noPo);
				request.setAttribute("tanggal_po", tanggalPo);
				request.setAttribute("kode_barang", kodeBarang);
				request.setAttribute("quantity", qty);
				request.setAttribute("tipe_bayar", tipeBayar);
				request.setAttribute("catatan", catatan);
				request.setAttribute("message",
						"Stock Order has been modified successfully");
				request.getRequestDispatcher(
						"/pages/so/stock-order-modify-success.jsp").forward(
						request, response);
			}

		} else {

			String tanggaSOAwal = request.getParameter("tanggal_so_awal");
			String tanggaSOAkhir = request.getParameter("tanggal_so_akhir");
			String nomorSO = request.getParameter("nomor_so");
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try {
				int total_pages = -1;
				Connection conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement(tableQueryCount);
				pstmt.setString(1, nomorSO);
				pstmt.setString(
						2,
						(tanggaSOAwal == null || tanggaSOAwal.equals("")) ? sdf
								.format(new Date(
										System.currentTimeMillis() + 86400000))
								: tanggaSOAwal);
				pstmt.setString(
						3,
						(tanggaSOAkhir == null || tanggaSOAkhir.equals("")) ? sdf
								.format(new Date(
										System.currentTimeMillis() + 86400000))
								: tanggaSOAkhir);
				pstmt.setString(4, UserSession.Factory.getUserSession(request).getUser().getKodeCabang());
				pstmt.setString(5, UserSession.Factory.getUserSession(request).getUser().getKodeCustomer());
				rs = pstmt.executeQuery();
				if (rs.next())
					total_pages = rs.getInt(1);
				rs.close();
				pstmt.close();

				conn = Connector.getInstance().getConnectionAdmin();
				pstmt = conn.prepareStatement("select * from (" + tableQuery
						+ ") as tbl where tbl.ROWNUMBER between ? and ?");
				pstmt.setString(1, nomorSO);
				pstmt.setString(
						2,
						(tanggaSOAwal == null || tanggaSOAwal.equals("")) ? sdf
								.format(new Date(
										System.currentTimeMillis() + 86400000))
								: tanggaSOAwal);
				pstmt.setString(
						3,
						(tanggaSOAkhir == null || tanggaSOAkhir.equals("")) ? sdf
								.format(new Date(
										System.currentTimeMillis() + 86400000))
								: tanggaSOAkhir);
				pstmt.setString(4, UserSession.Factory.getUserSession(request).getUser().getKodeCabang());
				pstmt.setString(5, UserSession.Factory.getUserSession(request).getUser().getKodeCustomer());
				pstmt.setInt(6, 0);
				pstmt.setInt(7, 10);
				rs = pstmt.executeQuery();

				ArrayList<String> tableColumn = new ArrayList<String>();
				ArrayList<ArrayList<String>> tableData = new ArrayList<ArrayList<String>>();
				int totalColumn = rs.getMetaData().getColumnCount();
				for (int i = 1; i < totalColumn; i++)
					tableColumn.add(rs.getMetaData().getColumnName(i));
				request.setAttribute("tableColumn", tableColumn);
				while (rs.next()) {
					ArrayList<String> rowData = new ArrayList<String>();
					for (int i = 1; i < totalColumn; i++) {
						rowData.add(rs.getString(i));
					}
					tableData.add(rowData);
				}
				request.setAttribute("tableData", tableData);
				request.setAttribute(
						"total_pages",
						Integer.toString((total_pages / 10)
								+ ((total_pages % 10) > 0 ? 1 : 0)));
			} catch (DaoException ex) {
				ex.printStackTrace();
				// logger.error(ex);
			} catch (NamingException ex) {
				ex.printStackTrace();
				// logger.error(ex);
			} catch (SQLException ex) {
				ex.printStackTrace();
				// logger.error(ex);
			} catch (Exception e) {
				e.printStackTrace();
			}
			request.getRequestDispatcher("/pages/so/stock-order-update.jsp")
					.forward(request, response);

		}
	}

}
