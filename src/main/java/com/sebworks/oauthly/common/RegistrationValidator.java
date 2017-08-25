package com.sebworks.oauthly.common;

import com.sebworks.oauthly.dto.RegistrationDto;
import com.sebworks.oauthly.repository.UserRepository;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Locale;

@Component
public class RegistrationValidator implements Validator {
    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean supports(Class<?> aClass) {
        return RegistrationDto.class.equals(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        RegistrationDto dto = (RegistrationDto) o;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "NotEmpty");
        if (dto.getUsername().length() < 3 || dto.getUsername().length() > 20) {
            errors.rejectValue("username", "Size.userForm.username");
        }
        // normalize username
        dto.setUsernameNormalized(dto.getUsername().replaceAll("[-\\\\.]","_").toLowerCase(Locale.ENGLISH));
        if (userRepository.findByUsernameNormalized(dto.getUsernameNormalized()) != null) {
            errors.rejectValue("username", "Duplicate.userForm.username");
        }
        if(!EmailValidator.getInstance().isValid(dto.getEmail())){
            errors.rejectValue("email", "Invalid.userForm.email");
        }
        // normalize email
        dto.setEmail(dto.getEmail().toLowerCase(Locale.ENGLISH));
        if (userRepository.findByEmail(dto.getEmail()) != null) {
            errors.rejectValue("email", "Duplicate.userForm.email");
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "NotEmpty");
        if (dto.getPassword().length() < 4 || dto.getPassword().length() > 32) {
            errors.rejectValue("password", "Size.userForm.password");
        }

        if (!dto.getPasswordConfirm().equals(dto.getPassword())) {
            errors.rejectValue("passwordConfirm", "Diff.userForm.passwordConfirm");
        }
    }
}