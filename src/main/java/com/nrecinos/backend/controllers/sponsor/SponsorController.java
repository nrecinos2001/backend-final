package com.nrecinos.backend.controllers.sponsor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nrecinos.backend.models.dtos.event.EventInfoDto;
import com.nrecinos.backend.models.dtos.general.MessageDto;
import com.nrecinos.backend.models.dtos.sponsor.CreateSponsorDto;
import com.nrecinos.backend.models.dtos.sponsor.SponsorInfoDto;
import com.nrecinos.backend.models.dtos.sponsor.UpdateSponsorDto;
import com.nrecinos.backend.models.entities.user.User;
import com.nrecinos.backend.repositories.UserRepository;
import com.nrecinos.backend.services.EventService;
import com.nrecinos.backend.services.SponsorService;
import com.nrecinos.backend.utils.JWTTools;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/sponsors")
public class SponsorController {
	@Autowired
	private SponsorService sponsorService;
	@Autowired
	private EventService eventService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private JWTTools jwtTools;

	@PostMapping("")
	ResponseEntity<?> create(@RequestBody @Valid CreateSponsorDto createSponsorDto, BindingResult validations, HttpServletRequest request){
		if(validations.hasErrors()) {
			return new ResponseEntity<>(validations.getAllErrors(), HttpStatus.BAD_REQUEST);
		}
		EventInfoDto event = eventService.findOne(createSponsorDto.getEventId());
		if (event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}
		String token = jwtTools.extractTokenFromRequest(request);
		String username = jwtTools.getUsernameFrom(token);
		User user = userRepository.findByUsernameOrEmail(username, username);
		if (user == null) {
			return new ResponseEntity<>(new MessageDto("User not found"), HttpStatus.NOT_FOUND);
		}
		if (event.getUser().getId() != user.getId()) {
			return new ResponseEntity<>(new MessageDto("Forbidden"), HttpStatus.FORBIDDEN);
		}
		SponsorInfoDto newSponsor = sponsorService.create(createSponsorDto);
		return new ResponseEntity<>(newSponsor, HttpStatus.CREATED);
	}
	
	@GetMapping("/event/{id}")
	ResponseEntity<?> getAll(@PathVariable(name = "id") Integer eventId){
		EventInfoDto event = eventService.findOne(eventId);
		if (event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}
		List<SponsorInfoDto> sponsors = sponsorService.findAllByEventId(eventId);
		return new ResponseEntity<>(sponsors, HttpStatus.OK);
	} 
	
	@GetMapping("/{code}")
	ResponseEntity<?> getSponsorById(@PathVariable(name = "code") Integer code){
		SponsorInfoDto sponsor = sponsorService.findOne(code); //TODO: Update with service method
		if(sponsor == null) {
			return new ResponseEntity<>(new MessageDto("Sponsor not Found"), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(sponsor, HttpStatus.CREATED);
	}
	
	@PatchMapping("/{code}")
	ResponseEntity<?> updateSponsor(@PathVariable(name = "code") Integer code, @RequestBody @Valid UpdateSponsorDto updateSponsorDto, BindingResult validations, HttpServletRequest request){
		if(validations.hasErrors()) {
			return new ResponseEntity<>(validations.getAllErrors(), HttpStatus.BAD_REQUEST);
		}
		String token = jwtTools.extractTokenFromRequest(request);
		String username = jwtTools.getUsernameFrom(token);
		SponsorInfoDto sponsor = sponsorService.findOne(code);
		if(sponsor == null) {
			return new ResponseEntity<>(new MessageDto("Sponsor not Found"), HttpStatus.NOT_FOUND);
		}
		EventInfoDto event = eventService.findOne(updateSponsorDto.getEventId());
		if (event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}			
		User user = userRepository.findByUsernameOrEmail(username, username);
		if (user == null) {
			return new ResponseEntity<>(new MessageDto("User not found"), HttpStatus.NOT_FOUND);
		}
		if (event.getUser().getId() != user.getId()) {
			return new ResponseEntity<>(new MessageDto("Forbidden"), HttpStatus.FORBIDDEN);
		}
		SponsorInfoDto sponsorUpdated = sponsorService.update(code, updateSponsorDto);
		return new ResponseEntity<>(sponsorUpdated, HttpStatus.OK);
	}
	
	@DeleteMapping("/{code}")
	ResponseEntity<?> delete(@PathVariable(name = "code") Integer code, HttpServletRequest request){
		String token = jwtTools.extractTokenFromRequest(request);
		String username = jwtTools.getUsernameFrom(token);
		SponsorInfoDto sponsor = sponsorService.findOne(code);
		if(sponsor == null) {
			return new ResponseEntity<>(new MessageDto("Sponsor not Found"), HttpStatus.NOT_FOUND);
		}
		EventInfoDto event = eventService.findOne(sponsor.getEventId());
		if (event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}			
		User user = userRepository.findByUsernameOrEmail(username, username);
		if (user == null) {
			return new ResponseEntity<>(new MessageDto("User not found"), HttpStatus.NOT_FOUND);
		}
		if (event.getUser().getId() != user.getId()) {
			return new ResponseEntity<>(new MessageDto("Forbidden"), HttpStatus.FORBIDDEN);
		}
		sponsorService.delete(code);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
