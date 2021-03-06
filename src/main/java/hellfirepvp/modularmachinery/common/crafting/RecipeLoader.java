/*******************************************************************************
 * HellFirePvP / Modular Machinery 2017
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.crafting;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapterAccessor;
import hellfirepvp.modularmachinery.common.crafting.adapter.RecipeAdapterRegistry;
import net.minecraft.util.JsonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: RecipeLoader
 * Created by HellFirePvP
 * Date: 27.06.2017 / 23:23
 */
public class RecipeLoader {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(MachineRecipe.MachineRecipeContainer.class, new MachineRecipe.Deserializer())
            .registerTypeHierarchyAdapter(RecipeAdapterAccessor.class, new RecipeAdapterAccessor.Deserializer())
            .create();

    public static List<RecipeAdapterAccessor> recipeAdapters = new LinkedList<>();
    private static Map<String, Exception> failedAttempts = new HashMap<>();
    public static String currentlyReadingPath = null;

    public static Map<FileType, List<File>> discoverDirectory(File directory) {
        Map<FileType, List<File>> candidates = new HashMap<>();
        for (FileType type : FileType.values()) {
            candidates.put(type, Lists.newLinkedList());
        }
        LinkedList<File> directories = Lists.newLinkedList();
        directories.add(directory);
        while (!directories.isEmpty()) {
            File dir = directories.remove(0);
            for (File f : dir.listFiles()) {
                if(f.isDirectory()) {
                    directories.addLast(f);
                } else {
                    if(FileType.ADAPTER.accepts(f.getName())) {
                        candidates.get(FileType.ADAPTER).add(f);
                    } else if(FileType.RECIPE.accepts(f.getName())) {
                        candidates.get(FileType.RECIPE).add(f);
                    }
                }
            }
        }
        return candidates;
    }

    public static List<MachineRecipe> loadRecipes(Map<RecipeLoader.FileType, List<File>> candidates) {
        recipeAdapters.clear();

        List<MachineRecipe> loadedRecipes = Lists.newArrayList();
        for (File f : candidates.get(FileType.RECIPE)) {
            currentlyReadingPath = f.getPath();
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(f))) {
                MachineRecipe.MachineRecipeContainer container = JsonUtils.fromJson(GSON, isr, MachineRecipe.MachineRecipeContainer.class);
                loadedRecipes.addAll(container.getRecipes());
            } catch (Exception exc) {
                failedAttempts.put(f.getPath(), exc);
            } finally {
                currentlyReadingPath = null;
            }
        }
        for (File f : candidates.get(FileType.ADAPTER)) {
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(f))) {
                RecipeAdapterAccessor accessor = JsonUtils.fromJson(GSON, isr, RecipeAdapterAccessor.class);
                Collection<MachineRecipe> recipes = accessor.loadRecipesForAdapter();
                if(recipes.isEmpty()) {
                    ModularMachinery.log.warn("Adapter with name " + accessor.getAdapterKey().toString() + " didn't provide have any recipes!");
                } else {
                    loadedRecipes.addAll(recipes);
                }
                recipeAdapters.add(accessor);
            } catch (Exception exc) {
                failedAttempts.put(f.getPath(), exc);
            }
        }
        return loadedRecipes;
    }

    public static Map<String, Exception> captureFailedAttempts() {
        Map<String, Exception> failed = failedAttempts;
        failedAttempts = new HashMap<>();
        return failed;
    }

    public static enum FileType {

        ADAPTER,
        RECIPE;

        public boolean accepts(String fileName) {
            switch (this) {
                case ADAPTER:
                    return fileName.endsWith(".adapter.json");
                case RECIPE:
                default:
                    return fileName.endsWith(".json");
            }
        }

    }

}
