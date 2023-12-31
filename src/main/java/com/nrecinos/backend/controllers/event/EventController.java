package com.nrecinos.backend.controllers.event;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nrecinos.backend.models.dtos.event.CreateEventDto;
import com.nrecinos.backend.models.dtos.event.EventInfoDto;
import com.nrecinos.backend.models.dtos.event.UpdateEventDto;
import com.nrecinos.backend.models.dtos.general.MessageDto;
import com.nrecinos.backend.models.entities.category.Category;
import com.nrecinos.backend.models.entities.event.Event;
import com.nrecinos.backend.models.entities.role.UserRoles;
import com.nrecinos.backend.models.entities.user.User;
import com.nrecinos.backend.repositories.CategoryRepository;
import com.nrecinos.backend.repositories.EventRepository;
import com.nrecinos.backend.repositories.UserRepository;
import com.nrecinos.backend.services.EventService;
import com.nrecinos.backend.utils.JWTTools;
import com.nrecinos.backend.utils.RoleValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@CrossOrigin("*")
@RequestMapping("/events")
public class EventController {
	@Autowired
	private EventService eventService;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private EventRepository eventRepository;
	
	@Autowired
	private JWTTools jwtTools;
	@Autowired
	private RoleValidator roleValidator;
		
	@GetMapping("")
	ResponseEntity<?> getAll(@RequestParam(defaultValue = "all", name = "status") String status) {
		List<EventInfoDto> events = eventService.findAll(status);
		return new ResponseEntity<>(events, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	ResponseEntity<?> getOne(@PathVariable(name = "id")Integer id){
		EventInfoDto event = eventService.findOne(id);
		if(event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(event, HttpStatus.OK);
	}
	
	@PostMapping("")
	ResponseEntity<?> create(@RequestBody @Valid CreateEventDto createEventDto, BindingResult validations, HttpServletRequest request) {
		if (validations.hasErrors()) {
			return new ResponseEntity<>(validations.getAllErrors(), HttpStatus.BAD_REQUEST);
		}
		List<String> roles = new ArrayList<String>();
		roles.add(UserRoles.ADMIN.getDisplayName());
		Boolean rolePermitted = roleValidator.validateRoles(roles, request);
		if (rolePermitted == false) {
			return new ResponseEntity<>(new MessageDto("Forbidden"), HttpStatus.FORBIDDEN);
		}
		String token = jwtTools.extractTokenFromRequest(request);
		String username = jwtTools.getUsernameFrom(token);
		User user = userRepository.findByUsernameOrEmail(username, username);
		Category category = categoryRepository.findOneById(createEventDto.getCategoryId());
		if (category == null) {
			return new ResponseEntity<>(new MessageDto("Category not found"), HttpStatus.NOT_FOUND);
		}
		eventService.create(createEventDto, user, category);
		return new ResponseEntity<>(new MessageDto("Event created"), HttpStatus.CREATED);
	}

	@PatchMapping("/{id}")
	ResponseEntity<?> update(@PathVariable(name = "id")Integer id, @RequestBody @Valid UpdateEventDto updateEventDto, BindingResult validations, HttpServletRequest request) {
		if (validations.hasErrors()) {
			return new ResponseEntity<>(validations.getAllErrors(), HttpStatus.BAD_REQUEST);
		}
		Event event = eventRepository.findOneById(id);
		if (event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}
		String token = jwtTools.extractTokenFromRequest(request);
		String username = jwtTools.getUsernameFrom(token);
		User user = userRepository.findByUsernameOrEmail(username, username);
		if (user.getId() != event.getUser().getId()) {
			return new ResponseEntity<>(new MessageDto("Forbidden"), HttpStatus.FORBIDDEN);
		}
		if (updateEventDto.getCategoryId() != null) {
        	Category category = categoryRepository.findOneById(updateEventDto.getCategoryId());
        	if (category == null) {
        		return new ResponseEntity<>(new MessageDto("Category not found"), HttpStatus.NOT_FOUND);
        	}
        }
		EventInfoDto eventUpdated = eventService.update(id, updateEventDto);
		return new ResponseEntity<>(eventUpdated, HttpStatus.OK);
	}
	
	@PatchMapping("/change-status/{id}")
	ResponseEntity<?> updateEventStatus(@PathVariable(name = "id") Integer id, HttpServletRequest request) {
		EventInfoDto event = eventService.findOne(id);
		if(event == null) {
			return new ResponseEntity<>(new MessageDto("Event not found"), HttpStatus.NOT_FOUND);
		}
		String token = jwtTools.extractTokenFromRequest(request);
		String username = jwtTools.getUsernameFrom(token);
		User user = userRepository.findByUsernameOrEmail(username, username);
		if (user.getId() != event.getUser().getId()) {
			return new ResponseEntity<>(new MessageDto("Forbidden"), HttpStatus.FORBIDDEN);
		}
		eventService.updateStatus(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
