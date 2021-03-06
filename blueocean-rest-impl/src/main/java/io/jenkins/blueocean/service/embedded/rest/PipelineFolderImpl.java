package io.jenkins.blueocean.service.embedded.rest;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.Reachable;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BlueActionProxy;
import io.jenkins.blueocean.rest.model.BlueFavorite;
import io.jenkins.blueocean.rest.model.BlueFavoriteAction;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.BluePipelineContainer;
import io.jenkins.blueocean.rest.model.BluePipelineFolder;
import io.jenkins.blueocean.rest.model.Resource;
import io.jenkins.blueocean.service.embedded.util.FavoriteUtil;
import org.kohsuke.stapler.json.JsonBody;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Vivek Pandey
 */
public class PipelineFolderImpl extends BluePipelineFolder {

    private final ItemGroup folder;
    private final Link parent;

    public PipelineFolderImpl(ItemGroup folder, Link parent) {
        this.folder = folder;
        this.parent = parent;
    }

    @Override
    public String getOrganization() {
        return OrganizationImpl.INSTANCE.getName();
    }

    @Override
    public String getName() {
        if(folder instanceof AbstractItem)
            return ((AbstractItem) folder).getName();
        else
            return folder.getDisplayName();
    }

    @Override
    public String getDisplayName() {
        return folder.getDisplayName();
    }

    @Override
    public String getFullName() {
        return folder.getFullName();
    }

    @Override
    public String getFullDisplayName() {
        return AbstractPipelineImpl.getFullDisplayName(folder, null);
    }

    @Override
    public Collection<BlueActionProxy> getActions() {
        return Collections.emptyList();
    }

    @Override
    public BluePipelineContainer getPipelines() {
        return new PipelineContainerImpl(folder, this);
    }

    @Override
    public Integer getNumberOfFolders() {
        int count=0;
        for(BluePipeline p:getPipelines ()){
            if(p instanceof BluePipelineFolder){
                count++;
            }
        }
        return count;
    }

    @Override
    public Integer getNumberOfPipelines() {
        int count=0;
        for(BluePipeline p:getPipelines ()){
            if(!(p instanceof BluePipelineFolder)){
                count++;
            }
        }
        return count;
    }


    @Override
    public BlueFavorite favorite(@JsonBody BlueFavoriteAction favoriteAction) {
        throw new ServiceException.MethodNotAllowedException("Cannot favorite a folder");
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        if(folder instanceof AbstractItem){
            AbstractItem item = (AbstractItem) folder;
            return AbstractPipelineImpl.getPermissions(item);
        }else{
            return null;
        }

    }

    @Override
    public Link getLink() {
        return OrganizationImpl.INSTANCE.getLink().rel("pipelines").rel(AbstractPipelineImpl.getRecursivePathFromFullName(this));
    }

    @Extension(ordinal = 0)
    public static class PipelineFactoryImpl extends BluePipelineFactory{

        @Override
        public PipelineFolderImpl getPipeline(Item item, Reachable parent) {
            if (item instanceof ItemGroup) {
                return new PipelineFolderImpl((ItemGroup) item, parent.getLink());
            }
            return null;
        }

        @Override
        public Resource resolve(Item context, Reachable parent, Item target) {
            PipelineFolderImpl folder = getPipeline(context, parent);
            if (folder!=null) {
                if(context == target){
                    return folder;
                }
                Item nextChild = findNextStep(folder.folder,target);
                for (BluePipelineFactory f : all()) {
                    Resource answer = f.resolve(nextChild, folder, target);
                    if (answer!=null)
                        return answer;
                }
            }
            return null;
        }
    }
}
