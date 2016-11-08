package com.nh.db.ml.simuservice.dockermgt.endpoints;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.model.Container;
import com.nh.db.ml.simuservice.dockergmgt.cli.CliConfSingleton;
import com.nh.db.ml.simuservice.dockermgt.exception.NoZeroStatusCode;
import com.nh.db.ml.simuservice.dockermgt.service.DockerService;
import com.nh.db.ml.simuservice.model.NbUsers;
import com.nh.db.ml.simuservice.model.SlaInfo;
import com.nh.db.ml.simuservice.model.Topo;

@Component
@Path("/")
public class DockerCTLEndpoints {
	private static final Logger LOGGER = LoggerFactory.getLogger(DockerCTLEndpoints.class);

	@Inject
	DockerService dockerService;

	// @POST
	// @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	// @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	// @Path("docker/grid")
	// public Response createGridSvg(Grid grid) {
	// LOGGER.debug(grid.getSessionId());
	// dockerService.createSvgFromGrid(grid);
	// return Response.accepted().build();
	// }
	//
	// @POST
	// @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	// @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	// @Path("docker/default")
	// public Response createDefault(String SessionID) {
	// LOGGER.debug("Request for default {}",SessionID);
	// dockerService.createSvgDefault(SessionID);
	// return Response.accepted().build();
	// }

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("docker/topo")
	public Response createTopo(Topo grid) throws InterruptedException {
		LOGGER.debug("Request for Topo {}", grid.getTopo());
		try {
			dockerService.createSvgFromTopo(grid);
			return Response.accepted().build();
		} catch (NoZeroStatusCode e) {
			throw new WebApplicationException("Docker return error", e, Response.status(Status.NOT_FOUND).build());
		}

	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("docker/sla")
	public Response createGridSvg(SlaInfo slaInfo) throws InterruptedException {
		try {
			dockerService.createSvgFromSla(slaInfo);
			return Response.accepted().build();
		} catch (NoZeroStatusCode e) {
			throw new WebApplicationException("Docker return error", e, Response.status(Status.NOT_FOUND).build());
		}
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("docker/LCsla")
	public Response findBestSLA(SlaInfo slaInfo) throws InterruptedException {
		try {
			dockerService.findBestSLA(slaInfo);
		} catch (NoZeroStatusCode e) {
			throw new WebApplicationException("Docker return error", e, Response.status(Status.NOT_FOUND).build());
		}
		return Response.accepted().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("topo")
	public Response getTopofile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail, 
			@FormDataParam("file") FormDataBodyPart body,
			@FormDataParam("filename") String filename, 
			@FormDataParam("sessionId") String sessionId) {
		try {
			java.nio.file.Path path = Paths.get(CliConfSingleton.folder,sessionId, filename);
			Paths.get(CliConfSingleton.folder,sessionId).toFile().mkdirs();
			OutputStream out = new FileOutputStream(path.toFile());
			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return Response.accepted().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("docker/users")
	public Response setBitrate(NbUsers nbUsers) {
		dockerService.setBitrate(nbUsers);
		return Response.accepted().build();
	}

	///////////////////////////////////////////////////////////////////////////////////
	//
	// From Mddash, throughput control example
	//
	///////////////////////////////////////////////////////////////////////////////////
	@GET
	public String message() {
		List<Container> containers = dockerService.getstatus();
		String response = "";
		for (Container container : containers) {
			response = response + " " + container.getNames()[0];
		}
		return response;
	}

	@POST
	@Path("{idDocker}/{bitrate}")
	public Response setBitrate(@PathParam("idDocker") String idDocker, @PathParam("bitrate") Integer bitrate) {
		dockerService.setBitrate(idDocker, bitrate);
		return Response.accepted().build();
	}

	@GET
	@Path("{idDocker}/{bitrate}")
	public Response setBitrate2(@PathParam("idDocker") String idDocker, @PathParam("bitrate") Integer bitrate) {
		dockerService.setBitrate(idDocker, bitrate);
		return Response.accepted().build();
	}

	@GET
	@Path("default")
	public Response setDefaultBitrate() {
		dockerService.setDefaultBitrate();
		return Response.accepted().build();
	}

}
