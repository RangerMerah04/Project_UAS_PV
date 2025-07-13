/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package e.kasir;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class EKasir {
    public static Connection con;
    public static Statement stm;
    public static void main(String[] args) {
        try{
            String url = "jdbc:mysql://localhost/sistem_gudang_kasir_db";
            String user = "root";
            String pass = "";
            
            con = DriverManager.getConnection(url, user, pass);
            stm = con.createStatement();
            System.out.println("Koneksi Berhasil!!");
        } catch(Exception e){
            System.err.println("Koneksi Gagal"+e.getMessage());
        }
    }
    
}
