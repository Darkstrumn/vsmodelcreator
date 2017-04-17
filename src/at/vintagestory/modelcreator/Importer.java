package at.vintagestory.modelcreator;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import at.vintagestory.modelcreator.enums.EnumEntityActivity;
import at.vintagestory.modelcreator.enums.EnumEntityActivityStoppedHandling;
import at.vintagestory.modelcreator.enums.EnumEntityAnimationEndHandling;
import at.vintagestory.modelcreator.model.Animation;
import at.vintagestory.modelcreator.model.AttachmentPoint;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.model.Face;
import at.vintagestory.modelcreator.model.Keyframe;
import at.vintagestory.modelcreator.model.KeyframeElement;
import at.vintagestory.modelcreator.model.PendingTexture;

public class Importer
{
	private Map<String, String> textureMap = new HashMap<String, String>();
	private String[] faceNames = { "north", "east", "south", "west", "up", "down" };

	
	private String inputPath;
	Project project;


	public Importer(String path)
	{
		this.inputPath = path;
	}

	public void ignoreTextureLoading()
	{
		//this.ignoreTextures = true;
	}

	public Project loadFromJSON()
	{
		project = new Project(inputPath);
		
		File path = new File(inputPath);
		if (path.exists() && path.isFile())
		{
			FileReader fr;
			BufferedReader reader;
			try
			{
				fr = new FileReader(path);
				reader = new BufferedReader(fr);
				readComponents(reader, path.getParentFile());
				reader.close();
				fr.close();
			}
			catch (Exception e)
			{
				EventQueue.invokeLater(new Runnable() {
			        @Override
			        public void run() {
						StackTraceElement[] elems = e.getStackTrace();
						String trace = "";
						for (int i = 0; i < elems.length; i++) {
							trace += elems[i].toString() + "\n";
							if (i >= 10) break;
						}
						
			        	JOptionPane.showMessageDialog(null, "Couldn't open this file, something unexpecteded happened\n\n" + e.toString() + "\nat\n" + trace);
			        	e.printStackTrace();
			        }
			        
			    });
				
				
			}
		}
		
		return project;
	}

	private void readComponents(BufferedReader reader, File dir) throws IOException
	{
		JsonParser parser = new JsonParser();
		JsonElement read = parser.parse(reader);

		if (read.isJsonObject())
		{
			JsonObject obj = read.getAsJsonObject();
			loadTextures(dir, obj);
			
			if (obj.has("elements") && obj.get("elements").isJsonArray())
			{
				JsonArray elements = obj.get("elements").getAsJsonArray();

				for (int i = 0; i < elements.size(); i++)
				{
					if (!elements.get(i).isJsonObject()) continue;
					
					Element elem = readElement(elements.get(i).getAsJsonObject(), null);
					if (elem != null) {
						project.rootElements.add(elem);
					}
				}
			}
			

			project.AmbientOcclusion = true; 
			
			if (obj.has("ambientocclusion") && obj.get("ambientocclusion").isJsonPrimitive())
			{
				project.AmbientOcclusion = obj.get("ambientocclusion").getAsBoolean();
			}
			
			if (obj.has("textureWidth") && obj.get("textureWidth").isJsonPrimitive())
			{
				project.TextureWidth = obj.get("textureWidth").getAsInt();
			}
			
			if (obj.has("textureHeight") && obj.get("textureHeight").isJsonPrimitive())
			{
				project.TextureHeight = obj.get("textureHeight").getAsInt();
			}
			
			if (obj.has("singleTexture") && obj.get("singleTexture").isJsonPrimitive())
			{
				project.SingleTexture = obj.get("singleTexture").getAsBoolean();
			}
			
			if (obj.has("allAngles") && obj.get("allAngles").isJsonPrimitive())
			{
				project.AllAngles = obj.get("allAngles").getAsBoolean();
			}
			
			if (obj.has("animations") && obj.get("animations").isJsonArray()) {
				JsonArray animations = obj.get("animations").getAsJsonArray();

				for (int i = 0; i < animations.size(); i++)
				{
					if (!animations.get(i).isJsonObject()) continue;
					
					Animation animation = readAnimation(animations.get(i).getAsJsonObject());
					if (animation != null) {
						project.Animations.add(animation);
					}
				}
			}
		}
	}


	private void loadTextures(File file, JsonObject obj)
	{
		if (obj.has("textures") && obj.get("textures").isJsonObject())
		{
			JsonObject textures = obj.get("textures").getAsJsonObject();

			for (Entry<String, JsonElement> entry : textures.entrySet())
			{
				if (entry.getValue().isJsonPrimitive())
				{
					String texture = entry.getValue().getAsString();

					if (texture.startsWith("#"))
					{
						textureMap.put(entry.getKey(), textureMap.get(texture.replace("#", "")));
					}
					else
					{						
						textureMap.put(entry.getKey().replace("#", ""), texture);
					}
					loadTexture(file, texture);
				}
			}
		}
	}

	private void loadTexture(File dir, String texture)
	{
		File assets = dir.getParentFile().getParentFile();
		//System.out.println("1." + assets.getAbsolutePath());
		if (assets != null)
		{
			File textureDir = new File(assets, "textures/");
			//System.out.println("3." + textureDir.getAbsolutePath());
			if (textureDir.exists() && textureDir.isDirectory())
			{
				File textureFile = new File(textureDir, texture + ".png");
				//System.out.println("4." + textureFile.getAbsolutePath());
				if (textureFile.exists() && textureFile.isFile())
				{
					project.PendingTextures.add(new PendingTexture(textureFile));
					return;
				}
			}
		}

		String texturePath = ModelCreator.prefs.get("texturePath", ".");
		
		if (new File(texturePath + File.separator + texture + ".png").exists())
		{
			project.PendingTextures.add(new PendingTexture(new File(texturePath + File.separator + texture + ".png")));
		}
	}

	private Animation readAnimation(JsonObject obj)
	{
		Animation anim = new Animation(obj.get("quantityframes").getAsInt());
		anim.setName(obj.get("name").getAsString());
		
		if (obj.has("keyframes") && obj.get("keyframes").isJsonArray()) {
			JsonArray keyframes = obj.get("keyframes").getAsJsonArray();

			int quantity = 0;
			for (int i = 0; i < keyframes.size(); i++)
			{
				if (!keyframes.get(i).isJsonObject()) continue;
				quantity++;
			}
			
			anim.keyframes = new Keyframe[quantity];

			
			for (int i = 0; i < keyframes.size(); i++)
			{
				if (!keyframes.get(i).isJsonObject()) continue;
				
				anim.keyframes[i] = readKeyframe(keyframes.get(i).getAsJsonObject());
			}
		}
		
		if (obj.has("forActivity") && obj.get("forActivity").isJsonPrimitive()) {
			anim.ForActivity = EnumEntityActivity.valueOf(obj.get("forActivity").getAsString());
		}

		if (obj.has("onActivityStopped") && obj.get("onActivityStopped").isJsonPrimitive()) {
			anim.OnActivityStopped = EnumEntityActivityStoppedHandling.valueOf(obj.get("onActivityStopped").getAsString());
		}

		if (obj.has("onAnimationEnd") && obj.get("onAnimationEnd").isJsonPrimitive()) {
			anim.OnAnimationEnd = EnumEntityAnimationEndHandling.valueOf(obj.get("onAnimationEnd").getAsString());
		}

		return anim;
	}

	
	private Keyframe readKeyframe(JsonObject obj)
	{
		Keyframe keyframe = new Keyframe(true);
		keyframe.setFrameNumber(obj.get("frame").getAsInt());
		
		if (obj.has("elements") && obj.get("elements").isJsonObject()) {
			
			Set<Entry<String, JsonElement>> elems = obj.get("elements").getAsJsonObject().entrySet();
			
			for (Entry<String, JsonElement> elem : elems) {
				KeyframeElement keyframeElem = readKeyframeElemenet(elem.getValue().getAsJsonObject(), elem.getKey());
				
				keyframe.AddElementFromImport(project, keyframeElem);
			}
		}
		
		return keyframe;
	}
	
	

	private KeyframeElement readKeyframeElemenet(JsonObject obj, String name)
	{
		KeyframeElement kelem = new KeyframeElement(true);
		
		kelem.AnimatedElementName = name; // obj.get("animatedElement").getAsString();
		
		if (obj.has("offsetX") || obj.has("offsetY") || obj.has("offsetZ")) {
			kelem.PositionSet = true;
			kelem.setOffsetX(obj.get("offsetX").getAsDouble());
			kelem.setOffsetY(obj.get("offsetY").getAsDouble());
			kelem.setOffsetZ(obj.get("offsetZ").getAsDouble());
		}
		
		if (obj.has("rotationX") || obj.has("rotationY") || obj.has("rotationZ")) {
			kelem.RotationSet = true;
			kelem.setRotationX(obj.get("rotationX").getAsDouble());
			kelem.setRotationY(obj.get("rotationY").getAsDouble());
			kelem.setRotationZ(obj.get("rotationZ").getAsDouble());
		}
		
		if (obj.has("stretchX") || obj.has("stretchY") || obj.has("stretchZ")) {
			kelem.StretchSet = true;
			kelem.setStretchX(obj.get("stretchX").getAsDouble());
			kelem.setStretchY(obj.get("stretchY").getAsDouble());
			kelem.setStretchZ(obj.get("stretchZ").getAsDouble());
		}
		
		return kelem;
	}

	private Element readElement(JsonObject obj, Element parent)
	{
		String name = "Element";
		JsonArray from = null;
		JsonArray to = null;

		if (obj.has("name") && obj.get("name").isJsonPrimitive())
		{
			name = obj.get("name").getAsString();
		}
		else if (obj.has("comment") && obj.get("comment").isJsonPrimitive())
		{
			name = obj.get("comment").getAsString();
		}
		if (obj.has("from") && obj.get("from").isJsonArray())
		{
			from = obj.get("from").getAsJsonArray();
		}
		if (obj.has("to") && obj.get("to").isJsonArray())
		{
			to = obj.get("to").getAsJsonArray();
		}

		if (from != null && to != null)
		{
			double x = from.get(0).getAsDouble();
			double y = from.get(1).getAsDouble();
			double z = from.get(2).getAsDouble();

			double w = to.get(0).getAsDouble() - x;
			double h = to.get(1).getAsDouble() - y;
			double d = to.get(2).getAsDouble() - z;

			Element element = new Element(w, h, d);
			element.setName(name);
			element.setStartX(x);
			element.setStartY(y);
			element.setStartZ(z);

			if (obj.has("rotationOrigin") && obj.get("rotationOrigin").isJsonArray())
			{
				JsonArray origin = obj.get("rotationOrigin").getAsJsonArray();
				double ox = origin.get(0).getAsDouble();
				double oy = origin.get(1).getAsDouble();
				double oz = origin.get(2).getAsDouble();

				element.setOriginX(ox);
				element.setOriginY(oy);
				element.setOriginZ(oz);
			}
			
		
			if (obj.has("rotationX") && obj.get("rotationX").isJsonPrimitive())
			{
				element.setRotationX(obj.get("rotationX").getAsDouble());
			}		

			if (obj.has("rotationY") && obj.get("rotationY").isJsonPrimitive())
			{
				element.setRotationY(obj.get("rotationY").getAsDouble());
			}
			
			if (obj.has("rotationZ") && obj.get("rotationZ").isJsonPrimitive())
			{
				element.setRotationZ(obj.get("rotationZ").getAsDouble());
			}		
			

			element.setShade(true);
			if (obj.has("shade") && obj.get("shade").isJsonPrimitive())
			{
				element.setShade(obj.get("shade").getAsBoolean());
			}
			
			if (obj.has("tintIndex") && obj.get("tintIndex").isJsonPrimitive())
			{
				element.setTintIndex(obj.get("tintIndex").getAsInt());
			}
			
			

			for (Face face : element.getAllFaces())
			{
				face.setEnabled(false);
			}

			if (obj.has("faces") && obj.get("faces").isJsonObject())
			{
				JsonObject faces = obj.get("faces").getAsJsonObject();

				for (String faceName : faceNames)
				{
					if (faces.has(faceName) && faces.get(faceName).isJsonObject())
					{
						readFace(faces.get(faceName).getAsJsonObject(), faceName, element);
					}
				}
			}
			

			if (obj.has("children") && obj.get("children").isJsonArray()) {
				JsonArray children = obj.get("children").getAsJsonArray();
				for(JsonElement child : children) {
					if (child.isJsonObject()) {
						element.ChildElements.add(readElement(child.getAsJsonObject(), element));
					}
				}
			
			}
			
			if (obj.has("attachmentpoints") && obj.get("attachmentpoints").isJsonArray()) {
				JsonArray children = obj.get("attachmentpoints").getAsJsonArray();
				for(JsonElement child : children) {
					if (child.isJsonObject()) {
						element.AttachmentPoints.add(readAttachmentPoint(child.getAsJsonObject()));
					}
				}
			}
			
			element.ParentElement = parent;
			
			element.setTexFromFace();
			
			return element;
		}
		return null;
	}
	

	private AttachmentPoint readAttachmentPoint(JsonObject obj)
	{
		AttachmentPoint point = new AttachmentPoint();
		
		point.setCode(obj.get("code").getAsString());
		point.setPosX(obj.get("posX").getAsDouble());
		point.setPosY(obj.get("posY").getAsDouble());
		point.setPosZ(obj.get("posZ").getAsDouble());
		point.setRotationX(obj.get("rotationX").getAsDouble());
		point.setRotationY(obj.get("rotationY").getAsDouble());
		point.setRotationZ(obj.get("rotationZ").getAsDouble());
		
		return point;
	}
	

	private void readFace(JsonObject obj, String name, Element element)
	{
		Face face = null;
		for (Face f : element.getAllFaces())
		{
			if (f.getSide() == Face.getFaceSide(name))
			{
				face = f;
			}
		}

		if (face != null)
		{
			face.setEnabled(true);

			// automatically set uv if not specified
			face.setEndU(element.getFaceDimension(face.getSide()).getWidth());
			face.setEndV(element.getFaceDimension(face.getSide()).getHeight());
			face.setAutoUVEnabled(true);

			if (obj.has("uv") && obj.get("uv").isJsonArray())
			{
				JsonArray uv = obj.get("uv").getAsJsonArray();

				double uStart = uv.get(0).getAsDouble();
				double vStart = uv.get(1).getAsDouble();
				double uEnd = uv.get(2).getAsDouble();
				double vEnd = uv.get(3).getAsDouble();

				face.setStartU(uStart);
				face.setStartV(vStart);
				face.setEndU(uEnd);
				face.setEndV(vEnd);
				face.setAutoUVEnabled(face.isCompatibleToAutoUV());
			}

			if (obj.has("texture") && obj.get("texture").isJsonPrimitive())
			{
				String loc = obj.get("texture").getAsString().replace("#", "");

				if (textureMap.containsKey(loc))
				{
					String tloc = textureMap.get(loc);
					String location = tloc.substring(0, tloc.lastIndexOf('/') + 1);
					String tname = tloc.replace(location, "");

					face.setTextureLocation(location);
					face.setTexture(tname);
				}
			}

			if (obj.has("rotation") && obj.get("rotation").isJsonPrimitive())
			{
				face.setRotation((int) obj.get("rotation").getAsDouble() / 90);
			}
			
			if (obj.has("glow") && obj.get("glow").isJsonPrimitive())
			{
				face.setGlow(((int) obj.get("glow").getAsInt()));
			}

			if (obj.has("enabled")) {
				boolean enabled = obj.get("enabled").getAsBoolean();
				face.setEnabled(enabled);
			}
		}
	}
}
