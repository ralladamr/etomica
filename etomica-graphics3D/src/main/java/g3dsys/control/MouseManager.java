/* Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package g3dsys.control;

import java.awt.Component;
import java.awt.Event;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class MouseManager implements MouseListener, MouseMotionListener {
	
  private G3DSys gsys;
  private Component c; // for giving focus on click

  private int previousDragX, previousDragY;

  private boolean rotationComplete = true;

  public MouseManager(Component component, G3DSys gs) {
    component.addMouseListener(this);
    component.addMouseMotionListener(this);
    c = component;
    gsys = gs;
  }

  /*
   * Mouse controls:
   * left drag - rotate
   * right drag - translate
   * ctrl+left drag - depth up/down
   * ctrl+right drag - slab up/down
   * shift+left drag - zoom up/down
   * shift+right drag - start rotating
   * ctrl+shift+left drag - z axis rotation
   */
  
  final static int LEFT = 16;
  final static int RIGHT = Event.META_MASK;  // 4
  final static int CTRL = Event.CTRL_MASK;   // 2
  final static int SHIFT = Event.SHIFT_MASK;
  
  private void mouseSinglePressDrag(MouseEvent e) {

	final int deltaX = e.getX() - previousDragX;
	final int deltaY = e.getY() - previousDragY;
	previousDragX = e.getX(); previousDragY = e.getY();
	    
    switch (e.getModifiers()) {
    case LEFT:
      // deltaY/X reversed here since
      // horizontal drag (x) : rotate around y, vertical (y) : rotate around x
      // also, /2 so we don't rotate too quickly
    	gsys.rotateByXY(deltaY/2.0f, deltaX/2.0f);
      gsys.fastRefresh();
      break;
    case RIGHT:
      gsys.xlateXY(deltaX, deltaY);
      gsys.fastRefresh();
      break;
    case CTRL|LEFT:
      gsys.setDepthPercent((float)gsys.getDepthPercent()+deltaY/4.0f);
      gsys.fastRefresh();
      break;
    case CTRL|RIGHT:
      gsys.setSlabPercent((float)gsys.getSlabPercent()+deltaY/4.0f);
      gsys.fastRefresh();
      break;
    case SHIFT|LEFT:
      gsys.zoomDown(deltaY);
      gsys.fastRefresh();
      break;
    case SHIFT|RIGHT:
      // Possible to go from one rotation to another without
      // a stop command in between.
      if(rotationComplete == true) {
    	    gsys.stopRotation();
    	    rotationComplete = false;
      }

      gsys.startRotation(deltaY, deltaX);

      break;
    case CTRL|SHIFT|LEFT:
      gsys.rotateByZ(-deltaX/2.0f); // - so left movement is counterclockwise
      gsys.fastRefresh();
      break;
    }
  }

  public void mousePressed(MouseEvent e) {
    c.requestFocus(); // Sun says this is not foolproof
    previousDragX = e.getX();
    previousDragY = e.getY();
  }

  public void mouseClicked(MouseEvent e) {

	  /*
	   * Mouse controls:
	   * shift+right - stop rotating
	   */

	  switch (e.getModifiers()) {

	    case SHIFT|RIGHT:
            gsys.stopRotation();
	    	break;
	    default:
		  break;
	  }
  }

  public void mouseDragged(MouseEvent e) {
    mouseSinglePressDrag(e);
  }

  public void mouseMoved(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  
  public void mouseReleased(MouseEvent e) {

	  /*
	   * Mouse controls:
	   * shift+right - rotating direction / speed complete
	   */

	  switch (e.getModifiers()) {

	    case SHIFT|RIGHT:
	    	rotationComplete = true;
	    	break;
	    default:
		  break;
	  }

  }
  
}
