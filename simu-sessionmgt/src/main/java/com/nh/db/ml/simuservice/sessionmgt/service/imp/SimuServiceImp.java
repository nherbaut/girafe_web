package com.nh.db.ml.simuservice.sessionmgt.service.imp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.db.ml.simuservice.model.Grid;
import com.nh.db.ml.simuservice.model.NbUsers;
import com.nh.db.ml.simuservice.model.SessionAndSvg;
import com.nh.db.ml.simuservice.model.SlaInfo;
import com.nh.db.ml.simuservice.sessionmgt.cli.CliConfSingleton;
import com.nh.db.ml.simuservice.sessionmgt.model.SessionSimu;
import com.nh.db.ml.simuservice.sessionmgt.repository.SessionSimuRepository;
import com.nh.db.ml.simuservice.sessionmgt.service.SimuService;

@Service
public class SimuServiceImp implements SimuService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimuServiceImp.class);
	@Inject
	SessionSimuRepository sessionSimuRepository;

	@Inject
	Client client;

	@Override
	public SessionAndSvg createTopoFromGrid(Grid grid) {
		SessionSimu session = new SessionSimu(UUID.randomUUID().toString());
		SessionAndSvg sessionAndSvg = new SessionAndSvg();
		sessionAndSvg.setSessionId(session.getSessionId());
		sessionAndSvg.setLinkSvg("");
		grid.setSessionId(session.getSessionId());
		try {
			session.setJsonGrid(new ObjectMapper().writeValueAsString(grid));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		sessionSimuRepository.save(session);
		WebTarget target = client.target("http://" + CliConfSingleton.simudocker + "/api/docker/grid");
		LOGGER.debug(target.getUri().toString());
		Response response = target.request().post(Entity.entity(grid, MediaType.APPLICATION_XML));

		return sessionAndSvg;
	}

	@Override
	public SessionAndSvg createTopo(Grid grid) {
		SessionSimu session = new SessionSimu(UUID.randomUUID().toString());
		SessionAndSvg sessionAndSvg = new SessionAndSvg();
		sessionAndSvg.setSessionId(session.getSessionId());
		sessionAndSvg.setLinkSvg("");
		grid.setSessionId(session.getSessionId());
		try {
			session.setJsonGrid(new ObjectMapper().writeValueAsString(grid));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		sessionSimuRepository.save(session);
		WebTarget target = client.target("http://" + CliConfSingleton.simudocker + "/api/docker/topo");
		LOGGER.debug(target.getUri().toString());
		Response response = target.request().post(Entity.entity(grid, MediaType.APPLICATION_XML));
		return sessionAndSvg;
	}

	@Override
	public SessionAndSvg createTopoDefault() {
		SessionSimu session = new SessionSimu(UUID.randomUUID().toString());
		SessionAndSvg sessionAndSvg = new SessionAndSvg();
		sessionAndSvg.setSessionId(session.getSessionId());
		sessionAndSvg.setLinkSvg("");

		sessionSimuRepository.save(session);
		WebTarget target = client.target("http://" + CliConfSingleton.simudocker + "/api/docker/default");
		Response response = target.request().post(Entity.entity(session.getSessionId(), MediaType.TEXT_PLAIN));
		return sessionAndSvg;
	}

	@Override
	public SlaInfo computeTopoFromSla(SlaInfo slaInfo) throws SimulationFailedException {
		SessionSimu session = sessionSimuRepository.findOneBySessionId(slaInfo.getSessionId());
		if (session != null) {
			Grid grid = null;
			if (session.getJsonGrid() != null) {
				try {
					grid = new ObjectMapper().readValue(session.getJsonGrid(), Grid.class);
				} catch (IOException e) {
					e.printStackTrace();
				}

				slaInfo.setTopo(grid.getTopo());
			}
			WebTarget target = client.target("http://" + CliConfSingleton.simudocker + "/api/docker/sla");
			Response response = target.request().post(Entity.entity(slaInfo, MediaType.APPLICATION_XML));
		}

		try {
			File file = new File(CliConfSingleton.folder + slaInfo.getSessionId() + "/solutions.data");
			FileReader fr = new FileReader(file);
			char[] a = new char[99999];
			fr.read(a);
			for (String d : String.valueOf(a).split("\n")) {
				if (d.contains("objective value:")) {
					slaInfo.setCosts(Double.valueOf(d.split("objective value:")[1]));
					return slaInfo;
				}
			}
		} catch (IOException fnfe) {
			throw new SimulationFailedException();
		}

		throw new SimulationFailedException();
	}

	@Override
	public SlaInfo computeLowCostSla(SlaInfo slaInfo) throws SimulationFailedException {
		SessionSimu session = sessionSimuRepository.findOneBySessionId(slaInfo.getSessionId());
		if (session != null) {
			Grid grid = null;
			if (session.getJsonGrid() != null) {
				try {
					grid = new ObjectMapper().readValue(session.getJsonGrid(), Grid.class);
				} catch (IOException e) {
					e.printStackTrace();
				}

				slaInfo.setTopo(grid.getTopo());
			}
			WebTarget target = client.target("http://" + CliConfSingleton.simudocker + "/api/docker/LCsla");
			Response response = target.request().post(Entity.entity(slaInfo, MediaType.APPLICATION_XML));
		}

		try {
			File file = new File(CliConfSingleton.folder + slaInfo.getSessionId() + "/best.mapping.data");
			FileReader fr = new FileReader(file);
			char[] a = new char[99999];
			fr.read(a);
			for (String d : String.valueOf(a).split("\n")) {
				if (!d.contains("CostFunction")) {
					slaInfo.setCosts(Double.valueOf(d.split(",")[2]));
					slaInfo.setVcdn(d.split(",")[1]);
					slaInfo.setVmg(d.split(",")[0]);
					return slaInfo;
				}
			}
		} catch (IOException fnfe) {
			throw new SimulationFailedException();
		}

		throw new SimulationFailedException();
	}

	@Override
	public void addUserForSession(NbUsers nbUsers) {
		SessionSimu session = sessionSimuRepository.findOneBySessionId(nbUsers.getSessionId());
		if (session != null) {
			Grid grid = null;
			// SlaInfo slaInfo = null;
			// try {
			// grid = new ObjectMapper().readValue(session.getJsonGrid(),
			// Grid.class);
			// slaInfo = new ObjectMapper().readValue(session.getJsonSla(),
			// SlaInfo.class);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// slaInfo.setGrid(grid.getX() + "x" + grid.getY());
			WebTarget target = client.target("http://" + CliConfSingleton.simudocker + "/api/docker/users");
			Response response = target.request().post(Entity.entity(nbUsers, MediaType.APPLICATION_XML));
		}
	}

	@Override
	public File getSvg(SessionAndSvg svgInfo) {
		File file = new File(CliConfSingleton.folder + svgInfo.getSessionId() + "/res.svg");

		return file;
	}

	@Override
	public byte[] getCsv(String sessionId) {
		String test = "UNIX,Valeur0101-0102,Valeur0101-0103,Valeur0102-0103,Valeur0201-0203\n"
				+ Long.toString(new Date().getTime() - 1000) + ",1,2,3,10\n" + new Date().getTime() + ",4,5,6,9";
		byte[] b = test.getBytes(StandardCharsets.UTF_8);

		return b;
	}

}
