package at.vintagestory.modelcreator.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import at.vintagestory.modelcreator.Exporter;
import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.gui.texturedialog.TextureDialog;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.util.screenshot.PendingScreenshot;
import at.vintagestory.modelcreator.util.screenshot.ScreenshotCallback;
import at.vintagestory.modelcreator.util.screenshot.Uploader;

public class GuiMain extends JMenuBar
{
	private static final long serialVersionUID = 1L;

	private ModelCreator creator;

	/* File */
	private JMenu menuFile;
	private JMenuItem itemNew;
	private JMenuItem itemLoad;
	private JMenuItem itemSave;
	private JMenuItem itemSaveAs;
	private JMenuItem itemTexturePath;
	private JMenuItem itemExit;

	/* Options */
	private JMenu menuOptions;
	private JCheckBoxMenuItem itemTransparency;
	private JCheckBoxMenuItem itemUnlockAngles;
	
	/* Add */
	private JMenu menuAdd;
	private JMenuItem itemAddCube;
	private JMenuItem itemAddFace;


	/* Other */
	private JMenu otherMenu;
	private JMenuItem itemSaveScreenshot;
	private JMenuItem itemReloadTextures;
	private JMenuItem itemImgurLink;
	private JMenuItem itemCredits;

	public GuiMain(ModelCreator creator)
	{
		this.creator = creator;
		initMenu();
	}

	private void initMenu()
	{
		menuFile = new JMenu("File");
		{
			itemNew = createItem("New", "New Model", KeyEvent.VK_N, new ImageIcon(getClass().getClassLoader().getResource("icons/new.png")));
			itemLoad = createItem("Open...", "Open JSON", KeyEvent.VK_I, new ImageIcon(getClass().getClassLoader().getResource("icons/import.png")));
			itemSave = createItem("Save...", "Save JSON", KeyEvent.VK_S, new ImageIcon(getClass().getClassLoader().getResource("icons/export.png")));
			itemSaveAs = createItem("Save as...", "Save JSON", KeyEvent.VK_E, new ImageIcon(getClass().getClassLoader().getResource("icons/export.png")));
			itemTexturePath = createItem("Set Texture Path...", "Set the base path to look for textures", KeyEvent.VK_P, new ImageIcon(getClass().getClassLoader().getResource("icons/texture.png")));
			itemExit = createItem("Exit", "Exit Application", KeyEvent.VK_Q, new ImageIcon(getClass().getClassLoader().getResource("icons/exit.png")));
		}

		menuOptions = new JMenu("Options");
		{
			itemTransparency = createCheckboxItem("Transparency", "Toggles transparent rendering in program", KeyEvent.VK_T, Icons.transparent);
			itemTransparency.setSelected(ModelCreator.transparent);
			
			itemUnlockAngles = createCheckboxItem("Unlock all Angles", "Disabling this allows angle stepping of single degrees. Suggested to unlock this only for entities.", KeyEvent.VK_A, Icons.transparent);
			itemUnlockAngles.setSelected(ModelCreator.unlockAngles);
		}

		menuAdd = new JMenu("Add");
		{
			itemAddCube = createItem("Add cube", "Add new cube", KeyEvent.VK_C, Icons.cube);
			itemAddFace = createItem("Add face", "Add single face", KeyEvent.VK_F, Icons.cube);
		}

		
		otherMenu = new JMenu("Other");
		{
			itemReloadTextures = createItem("Reload textures", "Reload textures", KeyEvent.VK_F5, Icons.new_);
			itemSaveScreenshot = createItem("Save Screenshot to Disk...", "Save screenshot to disk.", KeyEvent.VK_F12, Icons.disk);
			itemImgurLink = createItem("Get Imgur Link", "Get an Imgur link of your screenshot to share.", KeyEvent.VK_F11, Icons.imgur);
			itemCredits = createItem("Credits", "Who made this tol", 0, Icons.new_);
		}

		initActions();

	
		menuOptions.add(itemTransparency);
		menuOptions.add(itemUnlockAngles);
		
		menuAdd.add(itemAddCube);
		menuAdd.add(itemAddFace);

		otherMenu.add(itemReloadTextures);
		otherMenu.add(itemSaveScreenshot);
		otherMenu.add(itemImgurLink);
		otherMenu.add(itemCredits);

		menuFile.add(itemNew);
		menuFile.addSeparator();
		menuFile.add(itemLoad);
		menuFile.add(itemSave);
		menuFile.add(itemSaveAs);
		menuFile.addSeparator();
		menuFile.add(itemTexturePath);
		menuFile.addSeparator();
		menuFile.add(itemExit);

		add(menuFile);
		add(menuOptions);
		add(menuAdd);
		add(otherMenu);
	}

	private void initActions()
	{
		KeyStroke[] strokes = new KeyStroke[] {
			KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
			KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
			KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
			KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())
		};
		
		itemNew.setAccelerator(strokes[0]);
		itemLoad.setAccelerator(strokes[1]);
		itemSave.setAccelerator(strokes[2]);
		itemReloadTextures.setAccelerator(strokes[3]);
		

		ActionListener listener = a -> { OnNewModel(); }; 
		//registerKeyboardAction(listener, strokes[0], WHEN_IN_FOCUSED_WINDOW);
		itemNew.addActionListener(listener);


		listener = e -> { OnLoadFile(); };	
		//registerKeyboardAction(listener, strokes[1], WHEN_IN_FOCUSED_WINDOW);
		itemLoad.addActionListener(listener);
		

		listener = e -> {
			if (ModelCreator.currentProject.filePath == null) {
				creator.SaveProjectAs();
			} else {
				creator.SaveProject(new File(ModelCreator.currentProject.filePath));
			}
		};
		
		//registerKeyboardAction(listener, strokes[2], WHEN_IN_FOCUSED_WINDOW);
		itemSave.addActionListener(listener);
		

		itemSaveAs.addActionListener(e -> { creator.SaveProjectAs(); });

		
		
		listener = e ->
		{
			JFileChooser chooser = new JFileChooser(ModelCreator.prefs.get("texturePath", "."));
			chooser.setDialogTitle("Texture Path");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				ModelCreator.prefs.put("texturePath", chooser.getSelectedFile().getAbsolutePath());
			}
		};
		//registerKeyboardAction(listener, strokes[3], WHEN_IN_FOCUSED_WINDOW);
		itemTexturePath.addActionListener(listener);

		
		
		
		itemExit.addActionListener(e ->
		{
			creator.close();
		});

		itemTransparency.addActionListener(a ->
		{
			ModelCreator.transparent = itemTransparency.isSelected();
		});
		
		itemUnlockAngles.addActionListener(a ->
		{
			ModelCreator.unlockAngles = itemUnlockAngles.isSelected();
			ModelCreator.updateValues();
		});

		
		itemReloadTextures.addActionListener(a -> {
			TextureDialog.reloadTextures(creator);
		});

		itemSaveScreenshot.addActionListener(a ->
		{
			saveScreenshot();
			
		});

		itemImgurLink.addActionListener(a ->
		{
			CreateImgurLink();
			
		});
		
		itemCredits.addActionListener(a ->
		{
			CreditsDialog.show(creator);
		});

		
		itemAddCube.addActionListener(a ->
		{
			ModelCreator.currentProject.addElementAsChild(new Element(1, 1, 1));
		});
		
		itemAddFace.addActionListener(a ->
		{
			ModelCreator.currentProject.addElementAsChild(new Element(1, 1));
		});
	}
	
	

	private void OnLoadFile()
	{
		JFileChooser chooser = new JFileChooser(ModelCreator.prefs.get("filePath", "."));
		chooser.setDialogTitle("Input File");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setApproveButtonText("Open");

		FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON (.json)", "json");
		chooser.setFileFilter(filter);

		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			if (ModelCreator.currentProject.rootElements.size() > 0)
			{
				returnVal = JOptionPane.showConfirmDialog(null, "Your current project will be cleared, are you sure you want to continue?", "Warning", JOptionPane.YES_NO_OPTION);
			}
			
			if (returnVal != JOptionPane.NO_OPTION && returnVal != JOptionPane.CLOSED_OPTION)
			{
				String filePath = chooser.getSelectedFile().getAbsolutePath();					
				creator.LoadFile(filePath);					
			}
			
			ModelCreator.updateValues();
		}
	}

	private void OnNewModel()
	{
		int returnVal = JOptionPane.showConfirmDialog(creator, "You current work will be cleared, are you sure?", "Note", JOptionPane.YES_NO_OPTION);
		if (returnVal == JOptionPane.YES_OPTION)
		{
			creator.LoadFile(null);
		}
	}


	private JMenuItem createItem(String name, String tooltip, int mnemonic, Icon icon)
	{
		JMenuItem item = new JMenuItem(name);
		item.setToolTipText(tooltip);
		item.setMnemonic(mnemonic);
		item.setIcon(icon);
		return item;
	}
	
	private JCheckBoxMenuItem createCheckboxItem(String name, String tooltip, int mnemonic, Icon icon)
	{
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
		item.setToolTipText(tooltip);
		item.setMnemonic(mnemonic);
		item.setIcon(icon);
		return item;
	}
	
	
	


	private void saveScreenshot()
	{
		JFileChooser chooser = new JFileChooser(ModelCreator.prefs.get("filePath", "."));
		chooser.setDialogTitle("Output Directory");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setApproveButtonText("Save");

		FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG (.png)", "png");
		chooser.setFileFilter(filter);
		

		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			if (chooser.getSelectedFile().exists())
			{
				returnVal = JOptionPane.showConfirmDialog(null, "A file already exists with that name, are you sure you want to override?", "Warning", JOptionPane.YES_NO_OPTION);
			}
			if (returnVal != JOptionPane.NO_OPTION && returnVal != JOptionPane.CLOSED_OPTION)
			{
				String filePath = chooser.getSelectedFile().getAbsolutePath();
				if (!filePath.endsWith(".png")) {
					chooser.setSelectedFile(new File(filePath + ".png"));
				}
				
				ModelCreator.prefs.put("filePath", filePath);
				
				creator.modelrenderer.renderedLeftSidebar = null;
				creator.startScreenshot(new PendingScreenshot(chooser.getSelectedFile(), null));
			}
		}		
	}

	private void CreateImgurLink()
	{
		creator.modelrenderer.renderedLeftSidebar = null;
		creator.startScreenshot(new PendingScreenshot(null, new ScreenshotCallback()
		{
			@Override
			public void callback(File file)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							String url = Uploader.upload(file);

							JOptionPane message = new JOptionPane();
							String title;

							if (url != null && !url.equals("null"))
							{
								StringSelection text = new StringSelection(url);
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(text, null);
								title = "Success";
								message.setMessage("<html><b>" + url + "</b> has been copied to your clipboard.</html>");
							}
							else
							{
								title = "Error";
								message.setMessage("Failed to upload screenshot. Check your internet connection then try again.");
							}

							JDialog dialog = message.createDialog(GuiMain.this, title);
							dialog.setLocationRelativeTo(null);
							dialog.setModal(false);
							dialog.setVisible(true);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				});
			}
		}));
	}

}
