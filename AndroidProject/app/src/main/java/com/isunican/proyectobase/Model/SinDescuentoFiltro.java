package com.isunican.proyectobase.Model;

import com.isunican.proyectobase.Presenter.PresenterGasolineras;

import java.util.Iterator;

/**
 * Filtro que muestra solo las gasolineras que no tienen un descuento activo
 */
public class SinDescuentoFiltro implements IDescuentoFiltro {

    String nombre = "sinDescuento";

    @Override
    public void ordena(PresenterGasolineras presenterGasolineras) {
        Iterator i = presenterGasolineras.getGasolineras().iterator();

        while(i.hasNext()){
            Gasolinera g = (Gasolinera) i.next();
            if(g.getTieneDescuento()){
                i.remove();
            }
        }

    }

    @Override
    public String getNombre() {
        return null;
    }
}
