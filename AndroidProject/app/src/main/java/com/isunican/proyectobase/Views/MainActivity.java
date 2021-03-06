package com.isunican.proyectobase.Views;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.isunican.proyectobase.Model.Gasolinera;
import com.isunican.proyectobase.Model.ICombustibleFiltro;
import com.isunican.proyectobase.Model.IDescuentoFiltro;
import com.isunican.proyectobase.Model.IFiltro;
import com.isunican.proyectobase.Model.Posicion;
import com.isunican.proyectobase.Model.Vehiculo;
import com.isunican.proyectobase.Presenter.PresenterDescuentos;
import com.isunican.proyectobase.Presenter.PresenterFiltros;
import com.isunican.proyectobase.Presenter.PresenterGasolineras;
import com.isunican.proyectobase.Presenter.PresenterVehiculos;
import com.isunican.proyectobase.R;
import com.isunican.proyectobase.Utilities.Distancia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


/*
------------------------------------------------------------------
    Vista principal

    Presenta los datos de las gasolineras en formato lista.

------------------------------------------------------------------
*/
public class MainActivity extends AppCompatActivity {

    private PresenterGasolineras presenterGasolineras;

    private PresenterDescuentos presenterDescuentos;

    private PresenterVehiculos presenterVehiculos;

    private PresenterFiltros presenterFiltros;

    public boolean ubicacion;


    // Vista de lista y adaptador para cargar datos en ella
    private ListView listViewGasolineras;
    private RecyclerView recyclerViewFiltros;
    public ArrayAdapter<Gasolinera> adapter;
    private Button filter;
    private Button reset;


    private AdapterFiltros adapterFiltros;

    // Barra de progreso circular para mostar progeso de carga
    private ProgressBar progressBar;

    // Swipe and refresh (para recargar la lista con un swipe)
    SwipeRefreshLayout mSwipeRefreshLayout;


    private static final int PERMISSION_REQUEST = 100;
    private static final int REQUEST_CHECK_SETTINGS = 101;
    private FusedLocationProviderClient mFusedLocationClient;

    // Variables necesarias para mostrar el pop-up de añadir vehiculo
    private static final String POPUPPRIMERVEHICULO_TXT="/popUpPrimerVehiculo";
    private static final String ERROR_TAG = "Error";
    private static final String DATE = "dd/MM/yyyy HH:mm:ss";

    /**
     * onCreate
     *
     * Crea los elementos que conforman la actividad
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ubicacion=true;
        setContentView(R.layout.activity_main);
        this.presenterGasolineras = new PresenterGasolineras();

        this.presenterDescuentos = new PresenterDescuentos();
        presenterDescuentos.cargaDatosDummy();

        this.presenterVehiculos= new PresenterVehiculos();
        presenterVehiculos.cargaDatosVehiculos(PresenterVehiculos.getPath(MainActivity.this) + "/vehiculos.txt");
        presenterVehiculos.cargaVehiculoSeleccionado(PresenterVehiculos.getPath(MainActivity.this) + "/vehiculoSeleccionado.txt");

        this.presenterFiltros = new PresenterFiltros();

        // Obtenemos la vista de la lista
        listViewGasolineras = findViewById(R.id.listViewGasolineras);
        recyclerViewFiltros = findViewById(R.id.recyclerViewFiltros);

        recyclerViewFiltros.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        filter = findViewById(R.id.buttonFiltrar);
        reset = findViewById(R.id.buttonReset);

        // Barra de progreso
        // https://materialdoc.com/components/progress/
        progressBar = new ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        LinearLayout layout = findViewById(R.id.activity_precio_gasolina);
        layout.addView(progressBar, params);

        // Muestra el logo en el actionBar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.por_defecto_mod);

        // Swipe and refresh
        // Al hacer swipe en la lista, lanza la tarea asíncrona de carga de datos
        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onRefresh() {
                new CargaDatosGasolinerasTask(MainActivity.this).execute();
            }
        });

        if (!checkPermissionLocation()) {
            requestPermission();
        }
        invalidateOptionsMenu();
        // Al terminar de inicializar todas las variables
        // se lanza una tarea para cargar los datos de las gasolineras
        // Esto se ha de hacer en segundo plano definiendo una tarea asíncrona
        new CargaDatosGasolinerasTask(this).execute();

    }

    public void creaVehiculosParaTest(){
        presenterVehiculos.getVehiculos().add(new Vehiculo("Veh1"));
    }


    /**
     * Menú action bar
     *
     * Redefine métodos para el uso de un menú de tipo action bar.
     *
     * onCreateOptionsMenu
     * Carga las opciones del menú a partir del fichero de recursos menu/menu.xml
     *
     * onOptionsItemSelected
     * Define las respuestas a las distintas opciones del menú
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.itemGasolineras).setVisible(false);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.itemActualizar) {
            mSwipeRefreshLayout.setRefreshing(true);
            new CargaDatosGasolinerasTask(this).execute();
        } else if(item.getItemId()==R.id.itemMisVehiculos) {
            Intent myIntent = new Intent(MainActivity.this, MisVehiculosActivity.class);
            MainActivity.this.startActivity(myIntent);
        }else if (item.getItemId() == R.id.itemNuevoVehiculo) {
            Intent myIntent = new Intent(MainActivity.this, FormActivity.class);
            MainActivity.this.startActivity(myIntent);
        }else if (item.getItemId() == R.id.itemInfo) {
            Intent myIntent = new Intent(MainActivity.this, InfoActivity.class);
            MainActivity.this.startActivity(myIntent);
        }else if (item.getItemId() == R.id.itemFabrica) {
            try {
                presenterVehiculos.borra(PresenterVehiculos.getPath(MainActivity.this));
            } catch (IOException e) {
                Log.d("Borra", "No se ha podido borrar");
            }
            Intent myIntent = new Intent(MainActivity.this, MainActivity.class);
            MainActivity.this.startActivity(myIntent);
        }else if (item.getItemId() == R.id.itemDescuentos){
            Intent myIntent = new Intent(MainActivity.this, ListaDescuentosActivity.class);
            MainActivity.this.startActivity(myIntent);
        }

        return true;
    }




    /**
     * CargaDatosGasolinerasTask
     *
     * Tarea asincrona para obtener los datos de las gasolineras
     * en segundo plano.
     *
     * Redefinimos varios métodos que se ejecutan en el siguiente orden:
     * onPreExecute: activamos el dialogo de progreso
     * doInBackground: solicitamos que el presenter cargue los datos
     * onPostExecute: desactiva el dialogo de progreso,
     *    muestra las gasolineras en formato lista (a partir de un adapter)
     *    y define la acción al realizar al seleccionar alguna de ellas
     *
     * http://www.sgoliver.net/blog/tareas-en-segundo-plano-en-android-i-thread-y-asynctask/
     */
    public class CargaDatosGasolinerasTask extends AsyncTask<Void, Void, Boolean> {


        Activity activity;

        /**
         * Constructor de la tarea asincrona
         * @param activity
         */
        public CargaDatosGasolinerasTask(Activity activity) {
            this.activity = activity;
        }

        /**
         * onPreExecute
         * @deprecated deprecated method
         * Metodo ejecutado de forma previa a la ejecucion de la tarea definida en el metodo doInBackground()
         * Muestra un diálogo de progreso
         */
        @Override @Deprecated
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar
        }

        /**
         * doInBackground
         *
         * Tarea ejecutada en segundo plano
         * Llama al presenter para que lance el método de carga de los datos de las gasolineras
         * @param params
         * @return boolean
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            presenterGasolineras.cargaDatosGasolineras();
            for(Gasolinera g:presenterGasolineras.getGasolineras()){
                comparaRotulos(g);
            }
            //Aplicamos 3 descuentos del 30% a 3 gasolineras al azar
            presenterGasolineras.getGasolineras().get(5).setDescuento(presenterDescuentos.getDescuentos().get(4));
            presenterGasolineras.getGasolineras().get(8).setDescuento(presenterDescuentos.getDescuentos().get(4));
            presenterGasolineras.getGasolineras().get(10).setDescuento(presenterDescuentos.getDescuentos().get(4));
            return true;
        }

        /**
         * onPostExecute
         * @deprecated deprecated method
         * Se ejecuta al finalizar doInBackground
         * Oculta el diálogo de progreso.
         * Muestra en una lista los datos de las gasolineras cargadas,
         * creando un adapter y pasándoselo a la lista.
         * Define el manejo de la selección de los elementos de la lista,
         * lanzando con una intent una actividad de detalle
         * a la que pasamos un objeto Gasolinera
         *
         * @param res
         */
        @Override @Deprecated
        protected void onPostExecute(Boolean res) {
            Toast toast = null;
            for(Gasolinera g:presenterGasolineras.getGasolineras()){
                comparaRotulos(g);
            }
            //Aplicamos 3 descuentos del 30% a 3 gasolineras al azar
            presenterGasolineras.getGasolineras().get(5).setDescuento(presenterDescuentos.getDescuentos().get(4));
            presenterGasolineras.getGasolineras().get(8).setDescuento(presenterDescuentos.getDescuentos().get(4));
            presenterGasolineras.getGasolineras().get(10).setDescuento(presenterDescuentos.getDescuentos().get(4));
            // Si el progressDialog estaba activado, lo oculta
            progressBar.setVisibility(View.GONE);     // To Hide ProgressBar
            mSwipeRefreshLayout.setRefreshing(false);

            // Si se ha obtenido resultado en la tarea en segundo plano
            if (Boolean.TRUE.equals(res)) {
                // Definimos el array adapter
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                LocationRequest mLocationRequest = new LocationRequest();
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY));
                Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(MainActivity.this).checkLocationSettings(builder.build());
                result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                        try {
                            task.getResult(ApiException.class);
                            // All location settings are satisfied. The client can initialize location
                            // requests here.
                            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                requestPermission();
                            }
                            mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                                @Override
                                public void onComplete(@NonNull Task<Location> task)
                                {
                                    Location location = task.getResult();
                                    //Cuando el usuario tiene la ubicacion activada
                                    usaPosicion(location);

                                    adapter = new GasolineraArrayAdapter(activity, 0, presenterGasolineras.getGasolineras());
                                    listViewGasolineras.setAdapter(adapter);
                                    adapterFiltros.notifyDataSetChanged();
                                    Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.datosConUbicacion), Toast.LENGTH_LONG);
                                    toast.show();
                                }
                            });


                        } catch (ApiException exception) {
                            switchExcapcionLocation(exception);
                        }
                    }
                });

                adapter = new GasolineraArrayAdapter(activity, 0, presenterGasolineras.getGasolineras());

                comprobarFiltros();

                adapter = new GasolineraArrayAdapter(activity, 0, presenterGasolineras.getGasolineras());
                adapterFiltros = new AdapterFiltros(MainActivity.this, presenterFiltros.getListaFiltros());
                recyclerViewFiltros.setAdapter(adapterFiltros);
                adapterFiltros.notifyDataSetChanged();

                // Cargamos los datos en la lista
                if (!presenterGasolineras.getGasolineras().isEmpty()) {
                    // datos obtenidos con exito
                    listViewGasolineras.setAdapter(adapter);
                    toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.datos_exito), Toast.LENGTH_LONG);

                    //El siguiente metodo mostrará el Pop-Up solo si es necesario
                    mostrarPopUpPrimerVehiculo();

                } else {
                    // los datos estan siendo actualizados en el servidor, por lo que no son actualmente accesibles
                    // sucede en torno a las :00 y :30 de cada hora
                    toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.datos_no_accesibles), Toast.LENGTH_LONG);
                }
            } else {
                Intent myIntent = new Intent(MainActivity.this, NoDatosActivity.class);
                MainActivity.this.startActivity(myIntent);
                // error en la obtencion de datos desde el servidor
                toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.datos_no_obtenidos), Toast.LENGTH_LONG);
            }

            // Muestra el mensaje del resultado de la operación en un toast
            if (toast != null) {
                toast.show();
            }

            /*
             * Define el manejo de los eventos de click sobre elementos de la lista
             * En este caso, al pulsar un elemento se lanzará una actividad con una vista de detalle
             * a la que le pasamos el objeto Gasolinera sobre el que se pulsó, para que en el
             * destino tenga todos los datos que necesita para mostrar.
             * Para poder pasar un objeto Gasolinera mediante una intent con putExtra / getExtra,
             * hemos tenido que hacer que el objeto Gasolinera implemente la interfaz Parcelable
             */
            listViewGasolineras.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> a, View v, int position, long id) {

                    /* Obtengo el elemento directamente de su posicion,
                     * ya que es la misma que ocupa en la lista
                     */

                    Intent myIntent = new Intent(MainActivity.this, DetailActivity.class);
                    myIntent.putExtra(getResources().getString(R.string.pasoDatosGasolinera),
                            presenterGasolineras.getGasolineras().get(position));
                    MainActivity.this.startActivity(myIntent);

                }
            });

            filter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                    intent.putExtra("GasoleoA", presenterFiltros.getGasoleoA());
                    intent.putExtra("Gasolina95",presenterFiltros.getGasolina95());
                    intent.putExtra("DescuentoSI",presenterFiltros.getDescuentoSi());
                    intent.putExtra("DescuentoNo",presenterFiltros.getDescuentoNo());
                    setResult(Activity.RESULT_OK, intent);
                    MainActivity.this.startActivityForResult(intent, 10);
                }
            });
            /*
                On click para el botón Reset en el cual se desactivan todos los filtros
             */
            reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenterFiltros.setGasoleoA(false);
                    presenterFiltros.setGasolina95(false);
                    presenterFiltros.setDescuentoSi(false);
                    presenterFiltros.setDescuentoNo(false);
                    presenterFiltros.getListaFiltros().clear();
                    adapterFiltros.notifyDataSetChanged();
                    mSwipeRefreshLayout.setRefreshing(true);
                    new CargaDatosGasolinerasTask(MainActivity.this).execute();
                }
            });

        }

        private void usaPosicion(Location location) {
            if (location != null) {
                Posicion posUsuario = new Posicion(location.getLatitude(), location.getLongitude());

                for(Gasolinera g:presenterGasolineras.getGasolineras()){
                    g.setDistanciaEnKm(Distancia.distanciaKm(posUsuario,g.getPosicion()));
                    g.calculaPrecioFinal(PresenterVehiculos.getVehiculoSeleccionado());

                }
                comprobarFiltros();
            }
        }

        private void comprobarFiltros(){

            if(PresenterVehiculos.getVehiculoSeleccionado().getCombustible().equals("GasoleoA"))
                presenterFiltros.getFiltroGasoleA().ordena(presenterGasolineras.getGasolineras());
            else if(PresenterVehiculos.getVehiculoSeleccionado().getCombustible().equals("Gasolina95"))
                presenterFiltros.getFiltroGasolina95().ordena(presenterGasolineras.getGasolineras());

            if (presenterFiltros.getDescuentoSi()) {
                if (hayFiltro((IDescuentoFiltro.class)) == -1) {
                    presenterFiltros.getListaFiltros().add(presenterFiltros.getDescuentoSiFiltro());
                }
                presenterFiltros.getDescuentoSiFiltro().ordena(presenterGasolineras.getGasolineras());
            }

            if (presenterFiltros.getDescuentoNo()) {
                if (hayFiltro((IDescuentoFiltro.class)) == -1) {
                    presenterFiltros.getListaFiltros().add(presenterFiltros.getDescuentoNoFiltro());
                }
                presenterFiltros.getDescuentoNoFiltro().ordena(presenterGasolineras.getGasolineras());
            }

            if (presenterFiltros.getGasoleoA()) {
                if (hayFiltro((ICombustibleFiltro.class)) == -1) {
                    presenterFiltros.getListaFiltros().add(presenterFiltros.getFiltroGasoleA());
                }
                presenterFiltros.getFiltroGasoleA().ordena(presenterGasolineras.getGasolineras());
            }

            if (presenterFiltros.getGasolina95()) {
                if (hayFiltro((ICombustibleFiltro.class)) == -1) {
                    presenterFiltros.getListaFiltros().add(presenterFiltros.getFiltroGasolina95());
                }
                presenterFiltros.getFiltroGasolina95().ordena(presenterGasolineras.getGasolineras());
            }
        }

        private void switchExcapcionLocation(ApiException exception) {
            switch (exception.getStatusCode()) {
                case CommonStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied. But could be fixed by showing the
                    // user a dialog.
                    try {
                        // Cast to a resolvable exception.
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        if(ubicacion){
                            ubicacion=false;
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        }


                    } catch (IntentSender.SendIntentException|ClassCastException e) {
                        // Ignore the error.
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    // Location settings are not satisfied. However, we have no way to fix the
                    // settings so we won't show the dialog.
                    break;
                default:
                    break;
            }

        }

        private void comparaRotulos(Gasolinera g) {
            if (g.getRotulo().equals("CEPSA")) {
                g.setDescuento(presenterDescuentos.getDescuentos().get(0)); //Descuento del 10%
            }
        }


        /**
         * Método que muestra el pop-up de añadir vehiculo pop primera vez
         * solo si es necesario.
         */
        private void mostrarPopUpPrimerVehiculo() {

            //Tiempo transcurrido desde que se mostró anteriormente
            long tiempoTranscurrido=0;

            //Se carga la fecha de la última vez que se mostró
            Date ultimaFecha=cargarFechaPopUp();


            //Si no hay fecha guardada y solo está el vehiculo por defecto, se muestra el pop-up y se guarda la fecha
            if(ultimaFecha==null  && presenterVehiculos.getVehiculos().size()<=1){
                guardarFechaPopUp();
                Intent myIntent = new Intent(MainActivity.this, PopUpPrimerVehiculoActivity.class);
                MainActivity.this.startActivity(myIntent);
            }


            //Si hay una fecha guardada se muestra el pop-up solo si han transcurrido 24h
            if(ultimaFecha!=null){

                Date today=Calendar.getInstance().getTime();

                //Diferencia de tiempo entre la ultima vez que se mostró el pop up y la hora actual.
                long diffInMillies = Math.abs(today.getTime() - ultimaFecha.getTime());
                tiempoTranscurrido = TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS);


                //Si pasan mas de 24h se debe volver a mostrar el pop-up.
                if(tiempoTranscurrido>=120 && presenterVehiculos.getVehiculos().size()<=1){
                    guardarFechaPopUp();
                    Intent myIntent = new Intent(MainActivity.this, PopUpPrimerVehiculoActivity.class);
                    MainActivity.this.startActivity(myIntent);
                }

            }
        }

        /**
         * Método que carga de un archivo la última fecha en la
         * que se mostró el pop-up.
         * @return lastDate fecha en la que se mostró el pop-up por última vez
         */
        private Date cargarFechaPopUp(){

            Date lastDate=null;

            File tempFile = new File(MainActivity.this.getFilesDir()+POPUPPRIMERVEHICULO_TXT);
            boolean exists = tempFile.exists();

            if (exists){
                try (BufferedReader in = new BufferedReader(new FileReader(MainActivity.this.getFilesDir()+POPUPPRIMERVEHICULO_TXT))){

                    //Se lee la fecha con el formato adecuado y se cierra el fichero
                    String linea=in.readLine();
                    DateFormat df = new SimpleDateFormat(DATE);
                    lastDate = df.parse(linea);

                } catch(Exception e) {
                    Log.d(ERROR_TAG,"Error al cargar fecha pop-up");
                }
            }
            return lastDate;
        }


        /**
         * Método que guarda en el fichero la fecha actual
         */
        private void guardarFechaPopUp(){
            DateFormat df = new SimpleDateFormat(DATE);
            Date today = Calendar.getInstance().getTime();
            String reportDate = df.format(today);

            try (FileWriter fw = new FileWriter(new File(MainActivity.this.getFilesDir() + POPUPPRIMERVEHICULO_TXT))){
                fw.write(reportDate);
            }
            catch(IOException e) {
                Log.d(ERROR_TAG,"Error al guardar fecha en el fichero");
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 10 && resultCode == Activity.RESULT_OK && data != null){
            presenterFiltros.setGasoleoA(data.getBooleanExtra(FilterActivity.GASOLEOA, false));
            presenterFiltros.setGasolina95(data.getBooleanExtra(FilterActivity.GASOLINA95, false));
            presenterFiltros.setDescuentoNo(data.getBooleanExtra(FilterActivity.DESCUENTONO, false));
            presenterFiltros.setDescuentoSi(data.getBooleanExtra(FilterActivity.DESCUENTOSI, false));

            new CargaDatosGasolinerasTask(MainActivity.this).execute();
        }
        if(requestCode == 20 && resultCode == Activity.RESULT_OK && data != null){
            //Codigo para eliminar el filtro
            Log.d("ASD", PresenterFiltros.getFiltroMarcado().getText().toString());
            presenterFiltros.eliminaFiltroLista(PresenterFiltros.getFiltroMarcado().getText().toString());
            adapterFiltros.notifyDataSetChanged();

            new CargaDatosGasolinerasTask(MainActivity.this).execute();
        }

    }
    /*
        Método auxiliar que retorna -1 si no hay un filtro del tipo pasado como parámetro
        en el ArrayList de listaFiltros
     */
    public int hayFiltro(Class<? extends IFiltro> tipo){
        for(int i=0; i<presenterFiltros.getListaFiltros().size();i++){
            if(tipo.isAssignableFrom(presenterFiltros.getListaFiltros().get(i).getClass())){
                return i;
            }
        }
        return -1;
    }




    /*
    ------------------------------------------------------------------
        GasolineraArrayAdapter

        Adaptador para inyectar los datos de las gasolineras
        en el listview del layout principal de la aplicacion
    ------------------------------------------------------------------
    */
    public class GasolineraArrayAdapter extends ArrayAdapter<Gasolinera> {

        private final Context context;
        private final List<Gasolinera> listaGasolineras;

        // Constructor
        public GasolineraArrayAdapter(Context context, int resource, List<Gasolinera> objects) {
            super(context, resource, objects);
            this.context = context;
            this.listaGasolineras = objects;
        }

        // Llamado al renderizar la lista
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Obtiene el elemento que se está mostrando
            Gasolinera gasolinera = listaGasolineras.get(position);

            // Indica el layout a usar en cada elemento de la lista
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.item_gasolinera, null);
            // Asocia las variables de dicho layout
            ImageView logo = view.findViewById(R.id.imageViewLogo);
            TextView rotulo = view.findViewById(R.id.textViewRotulo);
            TextView direccion = view.findViewById(R.id.textViewDireccion);
            TextView textViewGasoleoA = view.findViewById(R.id.textViewGasoleoA);
            TextView textViewGasolina95 = view.findViewById(R.id.textViewGasolina95);

            view.setBackgroundColor(Color.WHITE);
            textViewGasoleoA.setTextColor(Color.BLACK);
            textViewGasolina95.setTextColor(Color.BLACK);

            if (gasolinera.getTieneDescuento()) {
                view.setBackgroundColor(0xfffffd82);
                textViewGasoleoA.setTextColor(Color.RED);
                textViewGasolina95.setTextColor(Color.RED);
            }
            // Y carga los datos del item
            rotulo.setText(gasolinera.getRotulo());
            direccion.setText(gasolinera.getDireccion());
            if (gasolinera.getTieneDescuento()) {
                textViewGasoleoA.setText(" " + Math.abs(gasolinera.getGasoleoAConDescuento()) + getResources().getString(R.string.moneda));
                textViewGasolina95.setText(" " + Math.abs(gasolinera.getGasolina95ConDescuento()) + getResources().getString(R.string.moneda));
            } else {
                textViewGasoleoA.setText(" " + Math.abs(gasolinera.getGasoleoA()) + getResources().getString(R.string.moneda));
                textViewGasolina95.setText(" " + Math.abs(gasolinera.getGasolina95()) + getResources().getString(R.string.moneda));
            }

            // carga icono
            cargaIcono(gasolinera, logo);


            // Si las dimensiones de la pantalla son menores
            // reducimos el texto de las etiquetas para que se vea correctamente
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            if (displayMetrics.widthPixels < 720) {
                TextView tv = view.findViewById(R.id.textViewGasoleoALabel);
                RelativeLayout.LayoutParams params = ((RelativeLayout.LayoutParams) tv.getLayoutParams());
                params.setMargins(15, 0, 0, 0);
                tv.setTextSize(11);
                TextView tmp;
                tmp = view.findViewById(R.id.textViewGasolina95Label);
                tmp.setTextSize(11);
                tmp = view.findViewById(R.id.textViewGasoleoA);
                tmp.setTextSize(11);
                tmp = view.findViewById(R.id.textViewGasolina95);
                tmp.setTextSize(11);
            }

            return view;
        }

        private void cargaIcono(Gasolinera gasolinera, ImageView logo) {
            String rotuleImageID = gasolinera.getRotulo().toLowerCase();

            // Tengo que protegerme ante el caso en el que el rotulo solo tiene digitos.
            // En ese caso getIdentifier devuelve esos digitos en vez de 0.
            int imageID = context.getResources().getIdentifier(rotuleImageID,
                    "drawable", context.getPackageName());

            if (imageID == 0 || TextUtils.isDigitsOnly(rotuleImageID)) {
                imageID = context.getResources().getIdentifier(getResources().getString(R.string.pordefecto),
                        "drawable", context.getPackageName());
            }
            logo.setImageResource(imageID);
        }
    }

    private boolean checkPermissionLocation() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION},PERMISSION_REQUEST );
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST && grantResults.length > 0){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(MainActivity.this, "Permisos concedidos, reinicie la app", Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(MainActivity.this, "Permisos no concedidos, la app no funcionara correctamente", Toast.LENGTH_SHORT).show();
                requestPermission();
            }
        }
    }


    /**
     * Método que se ejecuta cada vez que se vuelve a esta actividad
     */
    @Override
    public void onResume() {
        super.onResume();
        new CargaDatosGasolinerasTask(MainActivity.this).execute();
    }

}




class ViewHolderJr extends RecyclerView.ViewHolder{

    TextView nombreFiltro;
    Activity act;

    public ViewHolderJr(@NonNull View itemView, Activity act) {
        super(itemView);
        nombreFiltro = itemView.findViewById(R.id.txtNombreFiltro);
        nombreFiltro.setOnClickListener(filtroOnClickListener);
        this.act=act;
    }

    private View.OnClickListener filtroOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d("ASD", nombreFiltro.getText().toString());

            PresenterFiltros.setFiltroMarcado(nombreFiltro);
            Log.d("ASD", PresenterFiltros.getFiltroMarcado().getText().toString());

            Intent myIntent = new Intent(act, PopUpBorrarFiltroActivity.class);
            act.setResult(Activity.RESULT_OK, myIntent);
            act.startActivityForResult(myIntent, 20);
        }
    };

}

