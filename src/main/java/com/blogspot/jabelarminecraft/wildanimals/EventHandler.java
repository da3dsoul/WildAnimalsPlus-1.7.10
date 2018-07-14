/**
    Copyright (C) 2014 by jabelar

    This file is part of jabelar's Minecraft Forge modding examples; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    For a copy of the GNU General Public License see <http://www.gnu.org/licenses/>.
*/

package com.blogspot.jabelarminecraft.wildanimals;

import net.minecraft.entity.EntityList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;

import com.blogspot.jabelarminecraft.wildanimals.gui.WildAnimalsConfigGUI;

import cpw.mods.fml.client.GuiIngameModOptions;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class EventHandler 
{
    
    @SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
    public void onEvent(EntityJoinWorldEvent event)
    {
        // DEBUG
        if (EntityList.getStringFromID(event.entity.getEntityId()) != null)
        {
            System.out.println("Entity joined world = "+EntityList.getStringFromID(event.entity.getEntityId()));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
    public void onEvent(GuiOpenEvent event)
    {
        if (event.gui instanceof GuiIngameModOptions)
        {
            System.out.println("GuiOpenEvent for GuiIngameModOptions");
            event.gui = new WildAnimalsConfigGUI(null);        
        }
    }
}

