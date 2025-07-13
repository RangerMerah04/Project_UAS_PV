/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package e.kasir;

import java.sql.*;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;


public class kasir extends javax.swing.JFrame {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/sistem_gudang_kasir_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public kasir() {
        initComponents();
        loadNamaBarang();
        // Tambahkan ItemListener untuk cbNamaBarang
        cbNamaBarang.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                    String namaBarang = (String) cbNamaBarang.getSelectedItem();
                    if (namaBarang != null && !namaBarang.isEmpty()) {
                        setHargaBarang(namaBarang);
                    } else {
                        txtHarga.setText("");
                    }
                    // Reset txtQuantity agar kasir selalu input manual
                    txtQuantity.setText("");
                    // Reset info stok
                    lblStokBarang.setText("");
                    // Reset total harga
                    lblTotalHarga.setText("Rp.0");
                    // Update total harga otomatis
                    updateTotalHarga();
                }
            }
        });
        // Tambahkan DocumentListener pada txtQuantity untuk cek stok & total otomatis
        txtQuantity.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { cekStok(); updateTotalHarga(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { cekStok(); updateTotalHarga(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { cekStok(); updateTotalHarga(); }
        });
        // Tambahkan DocumentListener pada txtBayar untuk update lblkembalian otomatis
        txtBayar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateKembalian(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateKembalian(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateKembalian(); }
        });
        setTitle("Form Kasir");
        setLocationRelativeTo(null);
    }

    // Method untuk update lblkembalian otomatis saat user input bayar
    private void updateKembalian() {
        String totalStr = lblTotalHarga.getText().replaceAll("[^\\d.]", "");
        String bayarStr = txtBayar.getText().trim().replaceAll("[^\\d.]", "");
        if (totalStr.isEmpty() || bayarStr.isEmpty()) {
            lblkembalian.setText("Rp.0");
            return;
        }
        try {
            double total = Double.parseDouble(totalStr);
            double bayar = Double.parseDouble(bayarStr);
            if (bayar < total) {
                lblkembalian.setText("Uang bayar kurang!");
            } else {
                double kembalian = bayar - total;
                lblkembalian.setText(String.format("Rp %,.0f", kembalian));
            }
        } catch (NumberFormatException e) {
            lblkembalian.setText("Rp.0");
        }
    }

    // Method untuk update lblTotalHarga otomatis (format rupiah)
    private void updateTotalHarga() {
        String hargaStr = txtHarga.getText().trim();
        String qtyStr = txtQuantity.getText().trim();
        if (hargaStr.isEmpty() || qtyStr.isEmpty()) {
            lblTotalHarga.setText("Rp.0");
            return;
        }
        try {
            double harga = Double.parseDouble(hargaStr.replaceAll("[^\\d.]", ""));
            int qty = Integer.parseInt(qtyStr);
            if (harga > 0 && qty > 0) {
                double total = harga * qty;
                lblTotalHarga.setText(String.format("Rp %,.0f", total));
            } else {
                lblTotalHarga.setText("Rp.0");
            }
        } catch (NumberFormatException e) {
            lblTotalHarga.setText("Rp.0");
        }
    }

    // Method untuk set harga barang ke txtHarga berdasarkan nama barang
    private void setHargaBarang(String namaBarang) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT harga FROM barang_masuk WHERE nama_barang = ? ORDER BY tanggal_masuk DESC LIMIT 1";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, namaBarang);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                double harga = rs.getDouble("harga");
                txtHarga.setText(String.format("%.0f", harga));
            } else {
                txtHarga.setText("");
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            txtHarga.setText("");
            JOptionPane.showMessageDialog(this, "Gagal mengambil harga barang: " + e.getMessage());
        }
    }

    // Method untuk mengisi cbNamaBarang dari database
    private void loadNamaBarang() {
        cbNamaBarang.removeAllItems();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT nama_barang FROM barang_masuk GROUP BY nama_barang ORDER BY nama_barang ASC";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                cbNamaBarang.addItem(rs.getString("nama_barang"));
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat daftar barang: " + e.getMessage());
        }
        // Reset harga jika tidak ada barang dipilih
        txtHarga.setText("");
    }

    // Method untuk cek stok dan update lblStokBarang
    private void cekStok() {
        String namaBarang = cbNamaBarang.getSelectedItem() != null ? cbNamaBarang.getSelectedItem().toString() : "";
        String qtyStr = txtQuantity.getText().trim();
        if (namaBarang.isEmpty()) {
            lblStokBarang.setText("Pilih barang terlebih dahulu");
            return;
        }
        int stok = getStokBarang(namaBarang);
        if (qtyStr.isEmpty()) {
            lblStokBarang.setText("Stok tersedia: " + stok);
            return;
        }
        int qty = 0;
        try {
            qty = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            lblStokBarang.setText("Input jumlah tidak valid");
            return;
        }
        if (stok <= 0) {
            lblStokBarang.setText("Stok habis!");
        } else if (qty > stok) {
            lblStokBarang.setText("Stok tidak cukup! Stok tersedia: " + stok);
        } else {
            lblStokBarang.setText("Stok tersedia: " + stok);
        }
    }

    // Method untuk menghitung stok barang dari database
    private int getStokBarang(String namaBarang) {
        int masuk = 0, keluar = 0;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sqlMasuk = "SELECT COALESCE(SUM(jumlah),0) FROM barang_masuk WHERE nama_barang = ?";
            PreparedStatement pstMasuk = conn.prepareStatement(sqlMasuk);
            pstMasuk.setString(1, namaBarang);
            ResultSet rsMasuk = pstMasuk.executeQuery();
            if (rsMasuk.next()) masuk = rsMasuk.getInt(1);
            rsMasuk.close();
            pstMasuk.close();

            String sqlKeluar = "SELECT COALESCE(SUM(jumlah),0) FROM barang_keluar WHERE nama_barang = ?";
            PreparedStatement pstKeluar = conn.prepareStatement(sqlKeluar);
            pstKeluar.setString(1, namaBarang);
            ResultSet rsKeluar = pstKeluar.executeQuery();
            if (rsKeluar.next()) keluar = rsKeluar.getInt(1);
            rsKeluar.close();
            pstKeluar.close();
        } catch (Exception e) {
            lblStokBarang.setText("Gagal cek stok: " + e.getMessage());
        }
        return masuk - keluar;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtHarga = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtQuantity = new javax.swing.JTextField();
        lblTotalHarga = new javax.swing.JLabel();
        lblStokBarang = new javax.swing.JLabel();
        cbNamaBarang = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblOutput = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        txtBayar = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        btnBayar = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        lblkembalian = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu2 = new javax.swing.JMenu();
        menu_login = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(72, 166, 167));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Nama Barang");

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Harga (Rp)");

        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Quantity");

        txtHarga.setForeground(new java.awt.Color(51, 51, 0));

        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Total (Rp)");

        lblTotalHarga.setText("Rp.0");

        lblStokBarang.setText("null");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2)
                        .addComponent(jLabel3)
                        .addComponent(jLabel4)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(lblTotalHarga)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtQuantity, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                            .addComponent(txtHarga, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                            .addComponent(lblStokBarang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbNamaBarang, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(257, 257, 257))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtHarga, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(cbNamaBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblStokBarang)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalHarga)
                    .addComponent(jLabel5))
                .addGap(30, 30, 30))
        );

        tblOutput.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Nama barang", "Harga barang", "Quantity", "Total", "Bayar", "Kembalian"
            }
        ));
        jScrollPane1.setViewportView(tblOutput);

        jPanel2.setBackground(new java.awt.Color(154, 203, 208));

        txtBayar.setForeground(new java.awt.Color(51, 51, 0));

        jLabel6.setText("Bayar (Rp)");

        btnBayar.setText("Bayar");
        btnBayar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBayarActionPerformed(evt);
            }
        });

        jLabel7.setText("Kembalian (Rp)");

        lblkembalian.setText("Rp.0");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                .addComponent(lblkembalian, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(txtBayar, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(39, 39, 39))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(112, 112, 112))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(btnBayar)
                        .addGap(103, 103, 103))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtBayar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnBayar)
                .addGap(59, 59, 59)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lblkembalian))
                .addContainerGap(29, Short.MAX_VALUE))
        );

        jMenu2.setText("Back");

        menu_login.setText("Menu login");
        menu_login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_loginActionPerformed(evt);
            }
        });
        jMenu2.add(menu_login);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents



    private void menu_loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_loginActionPerformed
        // TODO add your handling code here:
        menu_login newWin = new menu_login();
        newWin.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_menu_loginActionPerformed

    private void btnBayarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBayarActionPerformed
        // Validasi input
        String namaBarang = cbNamaBarang.getSelectedItem() != null ? cbNamaBarang.getSelectedItem().toString().trim() : "";
        String hargaStr = txtHarga.getText().trim();
        String qtyStr = txtQuantity.getText().trim();
        String bayarStr = txtBayar.getText().trim();

        if (namaBarang.isEmpty() || hargaStr.isEmpty() || qtyStr.isEmpty() || bayarStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semua field wajib diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double harga, bayar, total, kembalian;
        int qty;

        try {
            harga = Double.parseDouble(hargaStr.replaceAll("[^\\d.]", ""));
            qty = Integer.parseInt(qtyStr);
            bayar = Double.parseDouble(bayarStr.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Harga, Quantity, dan Bayar harus berupa angka!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Cek stok cukup
        int stok = getStokBarang(namaBarang);
        if (qty > stok) {
            JOptionPane.showMessageDialog(this, "Stok tidak cukup! Stok tersedia: " + stok, "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (harga <= 0 || qty <= 0 || bayar <= 0) {
            JOptionPane.showMessageDialog(this, "Harga, Quantity, dan Bayar harus lebih dari 0!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        total = harga * qty;
        kembalian = bayar - total;

        if (bayar < total) {
            JOptionPane.showMessageDialog(this, "Uang bayar kurang dari total belanja!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Konfirmasi transaksi
        String konfirmasiMsg = String.format(
            "Konfirmasi Transaksi:\n\n" +
            "Nama Barang: %s\n" +
            "Harga Satuan: Rp %,.0f\n" +
            "Quantity: %d\n" +
            "Total: Rp %,.0f\n" +
            "Bayar: Rp %,.0f\n" +
            "Kembalian: Rp %,.0f\n\n" +
            "Apakah data sudah benar?",
            namaBarang, harga, qty, total, bayar, kembalian
        );

        int konfirmasi = JOptionPane.showConfirmDialog(this, konfirmasiMsg, "Konfirmasi Transaksi", JOptionPane.YES_NO_OPTION);
        if (konfirmasi != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Simpan ke tabel barang_keluar
            String sqlKeluar = "INSERT INTO barang_keluar (nama_barang, harga, jumlah, tanggal_keluar) VALUES (?, ?, ?, NOW())";
            PreparedStatement pstKeluar = conn.prepareStatement(sqlKeluar);
            pstKeluar.setString(1, namaBarang);
            pstKeluar.setDouble(2, harga);
            pstKeluar.setInt(3, qty);
            pstKeluar.executeUpdate();
            pstKeluar.close();

            // Simpan ke tabel penjualan
            String sqlPenjualan = "INSERT INTO penjualan (tanggal, total_barang, total_harga) VALUES (NOW(), ?, ?)";
            PreparedStatement pstPenjualan = conn.prepareStatement(sqlPenjualan, Statement.RETURN_GENERATED_KEYS);
            pstPenjualan.setInt(1, qty);
            pstPenjualan.setDouble(2, total);
            pstPenjualan.executeUpdate();

            int penjualanId = 0;
            ResultSet rsPenjualan = pstPenjualan.getGeneratedKeys();
            if (rsPenjualan.next()) {
                penjualanId = rsPenjualan.getInt(1);
            }
            rsPenjualan.close();
            pstPenjualan.close();

            // Simpan ke detail_penjualan
            String sqlDetail = "INSERT INTO detail_penjualan (penjualan_id, nama_barang, jumlah, harga_satuan, subtotal, bayar, kembalian) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstDetail = conn.prepareStatement(sqlDetail);
            pstDetail.setInt(1, penjualanId);
            pstDetail.setString(2, namaBarang);
            pstDetail.setInt(3, qty);
            pstDetail.setDouble(4, harga);
            pstDetail.setDouble(5, total);
            pstDetail.setDouble(6, bayar);
            pstDetail.setDouble(7, kembalian);
            pstDetail.executeUpdate();
            pstDetail.close();

            conn.close();

            // Update tabel output
            DefaultTableModel model = (DefaultTableModel) tblOutput.getModel();
            Object[] row = {
                namaBarang,
                String.format("Rp %,.0f", harga),
                qty,
                String.format("Rp %,.0f", total),
                String.format("Rp %,.0f", bayar),
                String.format("Rp %,.0f", kembalian)
            };
            model.insertRow(0, row);

            JOptionPane.showMessageDialog(this, "Transaksi berhasil disimpan ke barang keluar!");

            // Reset form
            cbNamaBarang.setSelectedIndex(-1);
            txtHarga.setText("");
            txtQuantity.setText("");
            txtBayar.setText("");
            lblkembalian.setText("Rp.0");
            lblTotalHarga.setText("Rp.0");
            lblStokBarang.setText("");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menyimpan transaksi: " + ex.getMessage());
        }

    }//GEN-LAST:event_btnBayarActionPerformed



    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(kasir.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(kasir.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(kasir.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(kasir.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new kasir().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBayar;
    private javax.swing.JComboBox<String> cbNamaBarang;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblStokBarang;
    private javax.swing.JLabel lblTotalHarga;
    private javax.swing.JLabel lblkembalian;
    private javax.swing.JMenuItem menu_login;
    private javax.swing.JTable tblOutput;
    private javax.swing.JTextField txtBayar;
    private javax.swing.JTextField txtHarga;
    private javax.swing.JTextField txtQuantity;
    // End of variables declaration//GEN-END:variables
}
