/*
 * uDig - User Friendly Desktop Internet GIS client http://udig.refractions.net (C) 2004,
 * Refractions Research Inc. This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; version 2.1 of the License. This library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package net.refractions.udig.project.internal.commands.edit;

import java.io.IOException;

import net.refractions.udig.core.IBlockingProvider;
import net.refractions.udig.core.internal.FeatureUtils;
import net.refractions.udig.project.EditFeature;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.command.MapCommand;
import net.refractions.udig.project.command.PostDeterminedEffectCommand;
import net.refractions.udig.project.command.UndoableMapCommand;
import net.refractions.udig.project.interceptor.InterceptorSupport;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Messages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;

/**
 * Deletes a feature from the map.
 * 
 * @author Jesse
 * @since 1.0.0
 */
public class DeleteFeatureCommand extends AbstractEditCommand implements UndoableMapCommand, 
    PostDeterminedEffectCommand {

    IBlockingProvider<SimpleFeature> featureProvider;

    private IBlockingProvider<ILayer> layerProvider;

    protected boolean done;

	private SimpleFeature feature;

	private ILayer oldLayer;

    /**
     * Construct <code>DeleteFeatureCommand</code>.
     */
    public DeleteFeatureCommand( IBlockingProvider<SimpleFeature> featureProvider, IBlockingProvider<ILayer> layerProvider ) {
        this.featureProvider = featureProvider;
        this.layerProvider = layerProvider;
    }

    /**
     * @see net.refractions.udig.project.command.MapCommand#run()
     */
    public void run( IProgressMonitor monitor ) throws Exception {
        execute(monitor);
    }

    public boolean execute( IProgressMonitor monitor ) throws Exception {
        monitor.beginTask(Messages.DeleteFeatureCommand_deleteFeature, 4); 
        monitor.worked(1);
        feature = featureProvider.get(new SubProgressMonitor(monitor, 1));
        if( feature==null ){
            return false; // most of been canceled
        }
        oldLayer = layerProvider.get(new SubProgressMonitor(monitor, 1));
        EditFeature editFeature = map.getEditManagerInternal().toEditFeature(feature,oldLayer);
        InterceptorSupport.runFeaturePreDeleteInterceprtors( editFeature );
        if( editFeature.hasError()){
            // unable to accept this editFeature (would be nice to pop open a dialog
            // here to review the EditFeature warnings and errors
            throw new IOException("Delete feature "+editFeature.getID()+" failed.");
        }
        
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        FeatureStore featureStore = oldLayer.getResource(FeatureStore.class, null);
        Filter filter = ff.id( feature.getIdentifier() );
        featureStore.removeFeatures( filter );
        
        InterceptorSupport.runFeatureDeletedInterceprtors( editFeature );
        if( editFeature.hasError()){
            // unable to accept this editFeature (would be nice to pop open a dialog
            // here to review the EditFeature warnings and errors
        }
        map.getEditManagerInternal().setEditFeature(null, null);
        
        monitor.done();
        return true;
    }
    public MapCommand copy() {
        return new DeleteFeatureCommand(featureProvider, layerProvider);
    }

    /**
     * @see net.refractions.udig.project.command.MapCommand#getName()
     */
    public String getName() {
        return Messages.DeleteFeatureCommand_deleteFeature; 
    }

    /**
     * @see net.refractions.udig.project.command.UndoableCommand#rollback()
     */
    public void rollback( IProgressMonitor monitor ) throws Exception {
        if( feature==null )
            return;
        map.getEditManagerInternal().setEditFeature(feature, (Layer) oldLayer);
        map.getEditManagerInternal().addFeature(feature, (Layer) oldLayer);
    }

}
