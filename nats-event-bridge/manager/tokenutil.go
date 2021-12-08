/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package manager

import (
	"errors"
	"strings"

	jwt "github.com/dgrijalva/jwt-go"
)

// GetUser gets sub claim from jwt token
func GetUser(s string) (string, error) {
	token, _, err := new(jwt.Parser).ParseUnverified(s, jwt.MapClaims{})
	if err != nil {
		return "", err
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok {
		claim, ok := claims["sub"]
		if !ok {
			return "", errors.New("sub claim not found in token")
		}

		sub := claim.(string)
		if sub == "" {
			return "", errors.New("Empty sub claim")
		}

		if strings.Contains(sub, ":") {
			fields := strings.Split(sub, ":")
			user := fields[len(fields)-1]

			return user, nil
		}

		return sub, nil
	}

	return "", errors.New("No claims found in token")
}
