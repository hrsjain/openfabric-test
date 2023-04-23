package ai.openfabric.api.controller;

import ai.openfabric.api.config.DockerConfig;
import ai.openfabric.api.model.Worker;
import ai.openfabric.api.repository.WorkerRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("${node.api.path}/worker")
public class WorkerController {

    @Autowired
    WorkerRepository workerRepository;

    @PostMapping(path = "/hello")
    public @ResponseBody String hello(@RequestBody String name) {
        return "Hello!" + name;
    }

    @PostMapping(path = "/create")
    public @ResponseBody String createContainer(@RequestBody String img_name) {

        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();

        Worker worker=null;
        try {
            CreateContainerResponse container
                    = dockerClient.createContainerCmd(img_name).exec();

            InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(container.getId()).exec();

            worker = new Worker();
            worker.setName(inspectContainerResponse.getName());
            worker.setContId(container.getId());
            worker.setStatus(inspectContainerResponse.getState().getStatus());
            worker.setIpAddress( inspectContainerResponse.getNetworkSettings().getIpAddress());
            worker.setGateway(inspectContainerResponse.getNetworkSettings().getGateway());
            worker.setCommand(inspectContainerResponse.getConfig().getCmd()[0]);
            worker.setEnvironment(inspectContainerResponse.getConfig().getEnv()[0]);
            worker.setImage_id(img_name);
            worker.onCreate();


            workerRepository.save(worker);

        }
        catch(NotFoundException e){
            e.printStackTrace();
            return "Image not found";
        }
        catch(NullPointerException e)
        {
            e.printStackTrace();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return "wrong Image_id. Check again...";
        }

        return "success, container id : " + worker.getId();
    }


    @PostMapping(path = "/start")
    public @ResponseBody String startContainer(@RequestBody String id) {

        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();

         try {

             Worker worker = workerRepository.findById(id).get();
             dockerClient.startContainerCmd(worker.getContId()).exec();
             worker.setStatus("running");
             worker.onUpdate();
             workerRepository.save(worker);

         }
        catch(NotFoundException e){
            e.printStackTrace();
             return "Container not found";
        }
         catch(NotModifiedException e){
             e.printStackTrace();
             return "Container is already running";
         }
        catch(Exception e)
        {
            e.printStackTrace();
            return "wrong Container_id. Check again...";
        }

        return "success";
    }

    @PostMapping(path = "/stop")
    public @ResponseBody String stopContainer(@RequestBody String id) {


        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();

        try{

            Worker worker = workerRepository.findById(id).get();
            dockerClient.stopContainerCmd(worker.getContId()).exec();
            worker.setStatus("exited");
            worker.onUpdate();
            workerRepository.save(worker);
        }
        catch(NotFoundException e){
            e.printStackTrace();
            return "Container not found";
        }
        catch(NotModifiedException e){
            e.printStackTrace();
            return "Container is already exited";
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return "wrong Container_id. Check again...";
        }

        return "success";
    }
    @PostMapping(path = "/kill")
    public @ResponseBody String killContainer(@RequestBody String id) {


        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();

        try {

            Worker worker = workerRepository.findById(id).get();
            dockerClient.killContainerCmd(worker.getContId()).exec();

            worker.setStatus("killed");
            worker.onUpdate();
            worker.ondelete();
            workerRepository.save(worker);
        }
        catch(NotFoundException e){
            e.printStackTrace();
            return "Container not found";
        }
        catch(NotModifiedException e){
            e.printStackTrace();
            return "Container is already killed";
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return "wrong Container_id. Check again...";
        }
        return "success";
    }
    @PostMapping(path = "/stats")
    public @ResponseBody Statistics stats(@RequestBody String id) {

        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();

            Worker worker = workerRepository.findById(id).get();
            InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
            dockerClient.statsCmd(worker.getContId()).exec(callback);
            Statistics stats=null;
            try {
                stats = callback.awaitResult();
                callback.close();
            } catch (RuntimeException | IOException e) {
                e.printStackTrace();
            }
            return stats;
    }


    @GetMapping(path = "/list")
    public @ResponseBody List<Container> listContainers() {

        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();

        List<Container> containers  = dockerClient.listContainersCmd().exec();

        return containers;
    }


    @PostMapping(path = "/info")
    public @ResponseBody InspectContainerResponse inspectContainer(@RequestBody String id) {

        DockerConfig dockerConfig = new DockerConfig();
        DockerClient dockerClient = dockerConfig.getdockerClient();
        InspectContainerResponse container=null;

        try {

            Worker worker = workerRepository.findById(id).get();
            container
                    = dockerClient.inspectContainerCmd(worker.getContId()).exec();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return container;
    }


}

