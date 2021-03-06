package com.isunican.proyectobase.Presenter;

import android.content.Context;
import android.os.Build;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.isunican.proyectobase.Model.Vehiculo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PresenterVehiculos {

    private List<Vehiculo> listVehiculos;

    private static final String ERROR_TAG = "Error";
    private static final String ERROR_CERRAR_FICHERO = "Error al cerrar el fichero";

    //Vehiculo seleccionado por el usuario. De este vehiculo se utilizará el desposito y consumo medio.
    private static Vehiculo vehiculoSeleccionado;

    public PresenterVehiculos(){
        this.listVehiculos=new ArrayList<>();
        Vehiculo v1= new Vehiculo("Vehiculo por defecto");
        Vehiculo v2=new Vehiculo("Tesla model S P100D");
        v1.setCombustible("GasoleoA");

        v1.setAnotaciones("Default");
        v2.setAnotaciones("Trabajo");

        v1.setDeposito(60);
        v1.setConsumoMedio(6.4);

        listVehiculos.add(v1);

    }

    public List<Vehiculo> getVehiculos(){
        return listVehiculos;
    }

    public static Vehiculo getVehiculoSeleccionado() {
        return vehiculoSeleccionado;
    }

    public static void setVehiculoSeleccionado(Vehiculo vehiculoSeleccionado) {
        PresenterVehiculos.vehiculoSeleccionado = vehiculoSeleccionado;
    }

    public static String getPath(Context context){
        return context.getFilesDir().toString();
    }

    public boolean guardaVehiculo(Vehiculo v, String path) {

        listVehiculos.add(v);
        StringBuilder bld = new StringBuilder();
        String output ="";

        for (Vehiculo v1:listVehiculos) {
            bld.append( "---\n" + v1.getModelo()+ "\n"+ v1.getDeposito() + "\n"+ v1.getConsumoMedio() + "\n"+ v1.getCombustible()
                    + "\n"+ v1.getAnotaciones()+ "\n");
        }

        bld.append("-fin-");
        output = bld.toString();

        try (FileWriter fw = new FileWriter(new File(path))){
            fw.write(output);
            return true;
        } catch(IOException e) {
            return false;
        }
    }

    public boolean cargaDatosVehiculos(String path) {

        List<Vehiculo> aux = new ArrayList<>();
        File tempFile = new File(path);
        boolean exists = tempFile.exists();

        if (exists){
            try (BufferedReader in =new BufferedReader(new FileReader(path))){

                String linea=in.readLine();
                Vehiculo v;

                while(linea.equals("---")){
                    v = new Vehiculo(in.readLine()); //modelo
                    v.setDeposito(Double.parseDouble(in.readLine()));//capacidad
                    v.setConsumoMedio(Double.parseDouble(in.readLine()));//c medio
                    v.setCombustible(in.readLine());//combust
                    v.setAnotaciones(in.readLine());//nota
                    aux.add(v);
                    linea = in.readLine();
                }
                listVehiculos = aux;

            } catch(Exception e) {
                Log.d(ERROR_TAG,"Error al cargar datos vehículo");
            }
        }
        return true;

    }

    public static void guardaVehiculoSeleccionado(Vehiculo v, String path) {

        String output = v.getModelo() + "\n" + v.getAnotaciones();

        try (FileWriter fw = new FileWriter(new File(path))){
            fw.write(output);
        } catch(IOException e) {
            Log.d(ERROR_TAG,ERROR_CERRAR_FICHERO);
        }

    }

    public boolean cargaVehiculoSeleccionado(String path) {

        List<Vehiculo> aux = new ArrayList<>();

        File tempFile = new File(path);
        boolean exists = tempFile.exists();
        if (exists){
            try (BufferedReader in = new BufferedReader(new FileReader(path ))){

                String modelo = in.readLine(); //modelo
                String anotacion = in.readLine();//nota

                for(Vehiculo v : listVehiculos){
                    if(v.getModelo().equals(modelo)){
                        aux.add(v);
                    }
                }
                if(aux.size() == 1)
                    setVehiculoSeleccionado(aux.get(0));
                else {
                    for(Vehiculo v : aux){
                        if(v.getAnotaciones().equals(anotacion)){
                            setVehiculoSeleccionado(v);
                        }
                    }
                }


            } catch(Exception e) {
                Log.d(ERROR_TAG, "Error al cargar el vehículo seleccionado");
            }
        }else{
            setVehiculoSeleccionado(listVehiculos.get(0));
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void borra(String path) throws IOException {
        Files.delete(Paths.get(path + "/vehiculos.txt"));
        Files.delete(Paths.get(path + "/vehiculoSeleccionado.txt"));
        setVehiculoSeleccionado(listVehiculos.get(0));

    }


}
