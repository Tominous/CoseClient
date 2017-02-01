package net.minecraft.client.renderer.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.src.Config;
import net.minecraft.src.RandomMobs;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;

public class TextureManager implements ITickable, IResourceManagerReloadListener
{
    private static final Logger logger = LogManager.getLogger();
    private final Map mapTextureObjects = Maps.newHashMap();
    private final List listTickables = Lists.newArrayList();
    private final Map mapTextureCounters = Maps.newHashMap();
    private IResourceManager theResourceManager;
    private static final String __OBFID = "CL_00001064";

    public TextureManager(IResourceManager p_i1284_1_)
    {
        this.theResourceManager = p_i1284_1_;
    }

    public void bindTexture(ResourceLocation resource)
    {
        if (Config.isRandomMobs())
        {
            resource = RandomMobs.getTextureLocation(resource);
        }

        Object var2 = (ITextureObject)this.mapTextureObjects.get(resource);

        if (var2 == null)
        {
            var2 = new SimpleTexture(resource);
            this.loadTexture(resource, (ITextureObject)var2);
        }

        TextureUtil.bindTexture(((ITextureObject)var2).getGlTextureId());
    }
    
    public void bindTexture(ResourceLocation loc, BufferedImage image)
    {
    	ITextureObject textureObject;
    	textureObject = new SimpleTexture(image);
    	loadTexture(loc, textureObject);
    	TextureUtil.bindTexture(textureObject.getGlTextureId());
    }

    public boolean loadTickableTexture(ResourceLocation p_110580_1_, ITickableTextureObject p_110580_2_)
    {
        if (this.loadTexture(p_110580_1_, p_110580_2_))
        {
            this.listTickables.add(p_110580_2_);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean loadTexture(ResourceLocation resource, final ITextureObject texture)
    {
        boolean loaded = true;
        Object texture2 = texture;

        try
        {
            texture.loadTexture(this.theResourceManager);
        }
        catch (IOException err1)
        {
            logger.warn("Failed to load texture: " + resource, err1);
            texture2 = TextureUtil.missingTexture;
            this.mapTextureObjects.put(resource, texture2);
            loaded = false;
        }
        catch (Throwable err2)
        {
            CrashReport report = CrashReport.makeCrashReport(err2, "Registering texture");
            CrashReportCategory rcategory = report.makeCategory("Resource location being registered");
            rcategory.addCrashSection("Resource location", resource);
            rcategory.addCrashSectionCallable("Texture object class", new Callable()
            {
                private static final String __OBFID = "CL_00001065";
                public String call()
                {
                    return texture.getClass().getName();
                }
            });
            throw new ReportedException(report);
        }

        this.mapTextureObjects.put(resource, texture2);
        return loaded;
    }

    public ITextureObject getTexture(ResourceLocation p_110581_1_)
    {
        return (ITextureObject)this.mapTextureObjects.get(p_110581_1_);
    }

    public ResourceLocation getDynamicTextureLocation(String p_110578_1_, DynamicTexture p_110578_2_)
    {
        Integer var3 = (Integer)this.mapTextureCounters.get(p_110578_1_);

        if (var3 == null)
        {
            var3 = Integer.valueOf(1);
        }
        else
        {
            var3 = Integer.valueOf(var3.intValue() + 1);
        }

        this.mapTextureCounters.put(p_110578_1_, var3);
        ResourceLocation var4 = new ResourceLocation(String.format("dynamic/%s_%d", new Object[] {p_110578_1_, var3}));
        this.loadTexture(var4, p_110578_2_);
        return var4;
    }

    public void tick()
    {
        Iterator var1 = this.listTickables.iterator();

        while (var1.hasNext())
        {
            ITickable var2 = (ITickable)var1.next();
            var2.tick();
        }
    }

    public void deleteTexture(ResourceLocation p_147645_1_)
    {
        ITextureObject var2 = this.getTexture(p_147645_1_);

        if (var2 != null)
        {
            TextureUtil.deleteTexture(var2.getGlTextureId());
        }
    }

    public void onResourceManagerReload(IResourceManager resourceManager)
    {
        Config.dbg("*** Reloading textures ***");
        Config.log("Resource packs: " + Config.getResourcePackNames());
        Iterator it = this.mapTextureObjects.keySet().iterator();

        while (it.hasNext())
        {
            ResourceLocation var2 = (ResourceLocation)it.next();

            if (var2.getResourcePath().startsWith("mcpatcher/"))
            {
                ITextureObject var3 = (ITextureObject)this.mapTextureObjects.get(var2);
                int glTexId = var3.getGlTextureId();

                if (glTexId > 0)
                {
                    GL11.glDeleteTextures(glTexId);
                }

                it.remove();
            }
        }

        Iterator var21 = this.mapTextureObjects.entrySet().iterator();

        while (var21.hasNext())
        {
            Entry var31 = (Entry)var21.next();
            this.loadTexture((ResourceLocation)var31.getKey(), (ITextureObject)var31.getValue());
        }
    }
}