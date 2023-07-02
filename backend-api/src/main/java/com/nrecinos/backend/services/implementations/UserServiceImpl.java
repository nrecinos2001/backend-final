package com.nrecinos.backend.services.implementations;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nrecinos.backend.models.dtos.user.CreateUserDto;
import com.nrecinos.backend.models.dtos.user.UpdateUserDto;
import com.nrecinos.backend.models.dtos.user.UpdateUserRoleDto;
import com.nrecinos.backend.models.dtos.user.UserInfoDto;
import com.nrecinos.backend.models.entities.role.Role;
import com.nrecinos.backend.models.entities.role.UserRoles;
import com.nrecinos.backend.models.entities.user.User;
import com.nrecinos.backend.models.entities.users_roles_role.UsersXRoles;
import com.nrecinos.backend.repositories.UserRepository;
import com.nrecinos.backend.repositories.UsersXRolesRepository;
import com.nrecinos.backend.services.RoleService;
import com.nrecinos.backend.services.UserService;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UsersXRolesRepository usersXRolesRepository;

	@Autowired
	RoleService roleService;
	@Autowired
	public PasswordEncoder passwordEncoder;
	
	@Override
	public UserInfoDto create(CreateUserDto createUserDto) {
		User createUser = new User(createUserDto.getName(), createUserDto.getLastname(), createUserDto.getPhoneNumber(), createUserDto.getEmail(), passwordEncoder.encode(createUserDto.getPassword()), createUserDto.getUsername(), false);
		User saveUser = this.save(createUser);
		UserInfoDto userInfo = this.serializeUserInfoDto(saveUser);
		Role userRole = roleService.getOneByName(UserRoles.USER.getDisplayName());
		UsersXRoles newRoleForUser = new UsersXRoles(saveUser, userRole);
		usersXRolesRepository.save(newRoleForUser);
		return userInfo;
	}

	@Override
	public User save(User user) {
		return userRepository.save(user);
	}

	@Override
	public List<UserInfoDto> findAll() {
		List<User> users = userRepository.findAll();
		List<UserInfoDto> usersInfoDto = users.stream()
				.map(u -> this.serializeUserInfoDto(u)).toList();
		return usersInfoDto;
	}

	@Override
	public UserInfoDto findOne(Integer code) {
		User user = userRepository.findOneById(code);
		if (user == null) {
			return null;
		}
		UserInfoDto userInfo = this.serializeUserInfoDto(user);
		return userInfo;
	}

	@Override
	public UserInfoDto update(Integer id, UpdateUserDto updateUserDto) {
		User userToUpdate = userRepository.findOneById(id);
		if (updateUserDto.getName() != null) {
			userToUpdate.setName(updateUserDto.getName());
		}
		if (updateUserDto.getLastname() != null) {
			userToUpdate.setLastname(updateUserDto.getLastname());
		}
		if (updateUserDto.getPhoneNumber() != null) {
			userToUpdate.setPhoneNumber(updateUserDto.getPhoneNumber());
		}
		if (updateUserDto.getEmail() != null) {
			userToUpdate.setEmail(updateUserDto.getEmail());
		}
		if (updateUserDto.getUsername() != null) {
			userToUpdate.setUsername(updateUserDto.getUsername());
		}
		User updatedUser = userRepository.save(userToUpdate);
		return this.serializeUserInfoDto(updatedUser);
	}

	@Override
	public UserInfoDto updateStatus(Integer id) {
		User user = userRepository.findOneById(id);
		if (user.getIsVerified() == true) {
			user.setIsVerified(false);
			User updatedUserToNonVerified = this.save(user);
			return this.serializeUserInfoDto(updatedUserToNonVerified);
		}
		user.setIsVerified(true);
		User updateUserToVerified = this.save(user);
		return this.serializeUserInfoDto(updateUserToVerified);
	}

	@Override
	public void delete(Integer id) {
		userRepository.deleteById(id);
		
	}

	@Override
	public UserInfoDto serializeUserInfoDto(User user) {
		List<String> roles = new ArrayList<>();
		if (!user.getUsersXRole().isEmpty()) {
			roles = user.getUsersXRole().stream().map(x -> x.getRole().getTitle()).toList();
		}
		return new UserInfoDto(user.getId(), user.getName(), user.getLastname(), user.getPhoneNumber(), user.getEmail(), user.getUsername(), user.getIsVerified(), roles);
	}

	@Override
	public UserInfoDto findByEmailOrUsername(String email, String username) {
		User user = userRepository.findByUsernameOrEmail(username, email);
		if (user != null) {			
			return this.serializeUserInfoDto(user);
		}
		return null;
	}
	
	@Override
	public String updatePassword(Integer id, String password) {
		User userToUpdate = userRepository.findOneById(id);
		userToUpdate.setPassword(passwordEncoder.encode(password));
		return "Password updated successfully";
	}

	@Override
	public String addRoleToUser(UpdateUserRoleDto addRoleDto) {
		User user = userRepository.findOneById(addRoleDto.getUserId());
		Role roleToUpdate = roleService.getOneByName(addRoleDto.getRole());
		UsersXRoles existingUserXRole = usersXRolesRepository.findOneByUserIdAndRoleId(addRoleDto.getUserId(), roleToUpdate.getId());
		if (existingUserXRole != null) {
			return null;
		}
		UsersXRoles newRole = new UsersXRoles(user, roleToUpdate);
		usersXRolesRepository.save(newRole);
		return "Role '" + roleToUpdate.getTitle() + "' was assigned correctly to the user";
	}

	@Override
	public String removeRoleFromUser(UpdateUserRoleDto removeRoleDto) {
		Role roleToRemove = roleService.getOneByName(removeRoleDto.getRole());
		UsersXRoles existingUserXRole = usersXRolesRepository.findOneByUserIdAndRoleId(removeRoleDto.getUserId(), roleToRemove.getId());
		if (existingUserXRole == null) {
			return null;
		}
		usersXRolesRepository.deleteById(existingUserXRole.getId());
		return "Role '" + roleToRemove .getTitle() + "' has been removed from user";
	}

}
