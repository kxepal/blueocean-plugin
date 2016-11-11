import { observable } from 'mobx';
import { Pager } from './Pager';
import { AppPaths, RestPaths } from '../utils/paths';
import { DataBunker } from '../model/DataBunker';
import { Fetch } from '../fetch';
import { BunkerService } from './BunkerService';
import { computed } from 'mobx';

export class ActivityService extends BunkerService {
  
    pagerKey(organization, pipeline) {
        return `Activities/${organization}-${pipeline}`;
    }
    activityPager(organization, pipeline) {
        return this.pagerService.getPager({
            key: this.pagerKey(organization, pipeline),
            lazyPager: () => new Pager(RestPaths.activities(organization, pipeline), 25, this),
        });
    }


    getOrAddActivity(activityData) {
        const activity = this.getItem(activityData._links.self.href);
        if (activity) {
            return activity;
        }
        
        return this.setItem(activityData);
    }

    bunkerMapper(data) {
        return this._mapQueueToPsuedoRun(data);
    }
    
    getActivity(href) {
        return this.getItem(href);
    }
    fetchActivity(href) {
        return Fetch.fetchJSON(href)
            .then(data => this.setItem(data));
    }
    /**
     * This function maps a queue item into a run instancce.
     *
     * We do this because the api returns us queued items as well
     * as runs and its easier to deal with them if they are modeled
     * as the same thing. If the raw data is needed if can be fetched
     * from _item.
     */
    _mapQueueToPsuedoRun(run) {
        if (run._class === 'io.jenkins.blueocean.service.embedded.rest.QueueItemImpl') {
            return {
                id: String(run.expectedBuildNumber),
                state: 'QUEUED',
                pipeline: run.pipeline,
                type: 'QueuedItem',
                result: 'UNKNOWN',
                job_run_queueId: run.id,
                enQueueTime: run.queuedTime,
                organization: run.organization,
                changeSet: [],
                _links: {
                    self: {
                        href: run._links.self.href,
                    },
                    parent: {
                        href: run._links.parent.href,
                    },
                },
                _item: run,
            };
        }
        return run;
    }

    getExpectedBuildNumber(event) {
        const runs = this._data.values();
        const eventJobUrl = event.blueocean_job_rest_url;
        let nextId = 0;
        for (let i = 0; i < runs.length; i++) {
            const run = runs[i];
            if (eventJobUrl !== run._links.parent.href) {
                // Not the same branch. Yes, run.pipeline actually contains
                // the branch name i.e. naming seems a bit confusing.
                continue;
            }
            if (run.job_run_queueId === event.job_run_queueId) {
                // We already have a "dummy" record for this queued job
                // run. No need to create another i.e. ignore this event.
                return run.id;
            }
            if (parseInt(run.id, 10) > nextId) { // figure out the next id, expectedBuildNumber
                nextId = parseInt(run.id, 10);
            }
        }

        return nextId + 1;
    }
}