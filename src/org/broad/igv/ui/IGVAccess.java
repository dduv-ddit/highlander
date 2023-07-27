/*****************************************************************************************
*
* Highlander - Copyright (C) <2012-2020> <Université catholique de Louvain (UCLouvain)>
* 	
* List of the contributors to the development of Highlander: see LICENSE file.
* Description and complete License: see LICENSE file.
* 	
* This program (Highlander) is free software: 
* you can redistribute it and/or modify it under the terms of the 
* GNU General Public License as published by the Free Software Foundation, 
* either version 3 of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program (see COPYING file).  If not, 
* see <http://www.gnu.org/licenses/>.
* 
*****************************************************************************************/

/**
*
* @author Raphael Helaers
*
*/

package org.broad.igv.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ToolTipManager;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.broad.igv.PreferenceManager;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.SequenceTrack;
import org.broad.igv.track.Track;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.Main.IGVArgs;
import org.broad.igv.ui.event.GlobalKeyDispatcher;
import org.broad.igv.util.HttpUtils;
import org.broad.igv.util.ResourceLocator;

public class IGVAccess {

	private static IGV igv = null;
	
	private static Frame igvFrame = null;
	
	public static void openIGV(Frame frame, String[] args, boolean alreadyOpen){
		if (!alreadyOpen){
	    // Turn on tooltip in case it was disabled for a temporary keyboard event, e.g. alt-tab
	    frame.addWindowListener(new WindowAdapter() {

	        @Override
	        public void windowActivated(WindowEvent e) {
	            ToolTipManager.sharedInstance().setEnabled(true);
	        }

	        @Override
	        public void windowGainedFocus(WindowEvent windowEvent) {
	            this.windowActivated(windowEvent);
	        }
	    });
		}

	    IGVArgs igvArgs = new IGVArgs(args);
	    
	    // Optional arguments
	    if (igvArgs.getPropertyOverrides() != null) {
	        PreferenceManager.getInstance().loadOverrides(igvArgs.getPropertyOverrides());
	    }
	    if (igvArgs.getDataServerURL() != null) {
	        PreferenceManager.getInstance().overrideDataServerURL(igvArgs.getDataServerURL());
	    }
	    if (igvArgs.getGenomeServerURL() != null) {
	        PreferenceManager.getInstance().overrideGenomeServerURL(igvArgs.getGenomeServerURL());
	    }


	    HttpUtils.getInstance().updateProxySettings();
	    
	    if (alreadyOpen){
	    	List<Track> tracks = igv.getAllTracks();
	    	for (Iterator<Track> it = tracks.iterator() ; it.hasNext() ; ){
	    		Track track = it.next();	
	    		if (track instanceof SequenceTrack || track instanceof FeatureTrack){
	    			if (!track.getName().endsWith(".vcf")) it.remove();
	    		}
	    	}
    		igv.removeTracks(tracks);
    		if (igvArgs.getDataFileString() != null) {
          // Not an xml file, assume its a list of data files
          String[] tokens = igvArgs.getDataFileString().split(",");
          String[] names = null;
          if (igvArgs.getName() != null) {
              names = igvArgs.getName().split(",");
          }

          String indexFile = igvArgs.getIndexFile();
          @SuppressWarnings({ "rawtypes", "unchecked" })
					List<ResourceLocator> locators = new ArrayList();
          int idx = 0;
          for (String p : tokens) {
              ResourceLocator rl = new ResourceLocator(p);
              if (names != null && idx < names.length) {
                  String name = names[idx];
                  rl.setName(name);
              }
              rl.setIndexPath(indexFile);
              locators.add(rl);
              idx++;
          }
          igv.loadResources(locators);
    		}
    		EventQueue.invokeLater(new Runnable() {
    	    @Override
    	    public void run() {
    	    	igvFrame.setVisible(true);
    	    	igvFrame.toFront();
    	    	igvFrame.setAlwaysOnTop(true);
    	    	igvFrame.setAlwaysOnTop(false);	    
    	    }
    		});
	    	setDividers();
	    	igv.goToLocus(args[1]);
	    }else{
	    	igvFrame = frame;
	    	igv = IGV.createInstance(frame);
	    	@SuppressWarnings("rawtypes")
				Future future = igv.startUp(igvArgs);
	    	try
	      {
	          future.get();
	      }
	      catch (InterruptedException e)
	      {
	          throw new RuntimeException(e);
	      }
	      catch (ExecutionException e)
	      {
	          throw new RuntimeException(e);
	      }
	    	//Should this be done here?  Will this step on other key dispatchers?
	    	KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new GlobalKeyDispatcher());
	    	igvFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
	    	igvFrame.toFront();
	    	igvFrame.setAlwaysOnTop(true);
	    	igvFrame.setAlwaysOnTop(false);
	    	setDividers();
	    }    	
	 }
	
	public static void setDividers(){
		double[] div = igv.getMainPanel().getDividerFractions();
		double part = 0.7 / (div.length-1);
		double val = 0.2;
    for (int i=0 ; i < div.length ; i++){
    	if (i > 0) val += part;
  		div[i] = val;
    }
    igv.getMainPanel().setDividerFractions(div);
	}
	
	public static void setPosition(String chr, int pos){
		igv.goToLocus(chr + ":"+pos);    
		EventQueue.invokeLater(new Runnable() {
	    @Override
	    public void run() {
	    	igvFrame.toFront();
	    	igvFrame.setAlwaysOnTop(true);
	    	igvFrame.setAlwaysOnTop(false);	    
	    }
		});
	}
	
	public static boolean isPresent(){
		return (igvFrame != null && igvFrame.isVisible());
	}
}
