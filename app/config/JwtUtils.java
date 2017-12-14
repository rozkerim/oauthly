package config;


import com.auth0.jwt.JWTSigner;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.JWTVerifyException;
import com.typesafe.config.Config;
import dtos.Token;
import dtos.TokenStatus;
import models.Client;
import models.Grant;
import models.User;
import repositories.ClientRepository;
import repositories.GrantRepository;
import repositories.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import play.Logger;
import scala.Tuple2;

@Singleton
public class JwtUtils {

    private final int expireCookie;
    private final int expireResetCode;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final GrantRepository grantRepository;
    private final String jwtSecret;
    private final int expireAccessToken;
    private final int expireRefreshToken;
    private final int expireAuthorizationCode;

    @Inject
    public JwtUtils(Config config, UserRepository userRepository, ClientRepository clientRepository, GrantRepository grantRepository) {
        jwtSecret = config.getString("jwt.secret");
        expireCookie = config.getInt("jwt.expire.cookie");
        expireResetCode = config.getInt("jwt.expire.resetCode");
        expireAccessToken = config.getInt("jwt.expire.accessToken");
        expireRefreshToken = config.getInt("jwt.expire.refreshToken");
        expireAuthorizationCode = config.getInt("jwt.expire.authorizationCode");
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.grantRepository = grantRepository;
    }

    public String prepareResetCode(User user) {
        int hash = Objects.hash(user.getUsername(), user.getEmail(), user.getPassword(), user.getLastUpdateTime());
        final long iat = System.currentTimeMillis() / 1000L; // issued at claim
        final long exp = iat + expireResetCode; // expires claim
        final JWTSigner signer = new JWTSigner(jwtSecret);

        final HashMap<String, Object> claims = new HashMap<>();
        claims.put("vt", 5); //type=reset_code
        claims.put("exp", exp);
        claims.put("hash", hash);
        claims.put("user", user.getId());

        return signer.sign(claims);
    }

    public User validateResetCode(String reset_code){
        if(reset_code == null)
            return null;
        try {
            final JWTVerifier verifier = new JWTVerifier(jwtSecret);
            final Map<String,Object> claims = verifier.verify(reset_code);
            // first check version
            int type = (int) claims.get("vt");
            if(type != 5){
                return null;
            }
            // check expiry date
            int exp = (int) claims.get("exp");
            if(exp < (System.currentTimeMillis() / 1000L))
                return null;
            // check user
            String user_id = (String) claims.get("user");
            User user = userRepository.findById(user_id);
            if(user == null){
                return null;
            }
            // check hash value
            int receivedHash = (int) claims.get("hash");
            int correctHash = Objects.hash(user.getUsername(), user.getEmail(), user.getPassword(), user.getLastUpdateTime());
            if(receivedHash != correctHash) {
                return null;
            }
            return user;
        } catch (JWTVerifyException | SignatureException | InvalidKeyException | NullPointerException e) {
            return null;
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
            return null;
        }
    }

    public String prepareCookie(User user) {
        int hash = Objects.hash(user.getUsername(), user.getEmail(), user.getPassword());
        final long iat = System.currentTimeMillis() / 1000L; // issued at claim
        final long exp = iat + expireCookie; // expires claim
        final JWTSigner signer = new JWTSigner(jwtSecret);

        final HashMap<String, Object> claims = new HashMap<>();
        claims.put("vt", 4); //type=cookie_ltat
        claims.put("exp", exp);
        claims.put("hash", hash);
        claims.put("user", user.getId());

        return signer.sign(claims);
    }

    public User validateCookie(String cookie_value){
        if(cookie_value == null)
            return null;
        try {
            final JWTVerifier verifier = new JWTVerifier(jwtSecret);
            final Map<String,Object> claims = verifier.verify(cookie_value);
            // first check version
            int type = (int) claims.get("vt");
            if(type != 4){
                return null;
            }
            // check expiry date
            int exp = (int) claims.get("exp");
            if(exp < (System.currentTimeMillis() / 1000L))
                return null;
            // check user
            String user_id = (String) claims.get("user");
            User user = userRepository.findById(user_id);
            if(user == null){
                return null;
            }
            // check hash value
            int receivedHash = (int) claims.get("hash");
            int correctHash = Objects.hash(user.getUsername(), user.getEmail(), user.getPassword());
            if(receivedHash != correctHash) {
                return null;
            }
            return user;
        } catch (JWTVerifyException | SignatureException | InvalidKeyException | NullPointerException e) {
            return null;
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
            return null;
        }
    }

    public int getExpireCookie() {
        return expireCookie;
    }


    public String prepareCode(String client_id, String client_secret, String grant_id, String redirect_uri) {
        int hash = Objects.hash(client_id, client_secret);
        final long iat = System.currentTimeMillis() / 1000L; // issued at claim
        final long exp = iat + expireAuthorizationCode;
        final JWTSigner signer = new JWTSigner(jwtSecret);

        final HashMap<String, Object> claims = new HashMap<>();
        claims.put("vt", 3); //type=authorization_code
        claims.put("exp", exp);
        claims.put("h", hash);
        claims.put("g", grant_id);
        claims.put("r", redirect_uri);
        return signer.sign(claims);
    }

    public Grant validateCode(String code, String redirect_uri){
        if(code == null || redirect_uri == null)
            return null;
        try {
            final JWTVerifier verifier = new JWTVerifier(jwtSecret);
            final Map<String,Object> claims = verifier.verify(code);
            // first check version
            int type = (int) claims.get("vt");
            if(type != 3){
                return null;
            }
            // check expiry date
            int exp = (int) claims.get("exp");
            if(exp < (System.currentTimeMillis() / 1000L))
                return null;
            // check grant
            String grant_id = (String) claims.get("g");
            Grant grant = grantRepository.findById(grant_id);
            if(grant == null){
                return null;
            }
            // check hash value
            int receivedHash = (int) claims.get("h");
            Client client = clientRepository.findById(grant.getClientId());
            if(client == null){
                return null;
            }
            int correctHash = Objects.hash(client.getId(), client.getSecret());
            if(receivedHash != correctHash) {
                return null;
            }
            // check redirect_uri
            if(!Objects.equals(claims.get("r"), redirect_uri)){
                return null;
            }
            return grant;
        } catch (JWTVerifyException | SignatureException | InvalidKeyException | NullPointerException e) {
            return null;
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
            return null;
        }
    }


    /**
     * Prepares & issues a new token and returns it.
     * It puts the following information to the token:
     * <li>
     * <ul>version 'v'</ul>
     * <ul>expiry date 'exp'</ul>
     * <ul>combined hash 'h' value of client_id, client_secret, username and password.</ul><br>
     * This hash is important because if either one of the parameters change, the change in hash value will render the token invalid. Example: if password of user changes, the token will automatically get invalid. Note that this hash is not technically bulletproof, there exist very little (approx. 1 in 4*10^9) chance that it will compute the same hash value.
     * <ul>token type 't' -> 'a' (access) or 'r' (refresh)</ul>
     * </li>
     * It prepares both access and refresh tokens, signs them using JWT and packs in a Token object.
     * Note that this single user implementation does not put username to the claims map.
     *
     * @return generated token
     */
    public Token prepareToken(String client_id, String client_secret, String grant_id, List<String> scopes) {
        int hash = Objects.hash(client_id, client_secret);
        final long iat = System.currentTimeMillis() / 1000L; // issued at claim
        final long exp = iat + expireAccessToken; // expires claim. In this case the token expires in 60 seconds
        final JWTSigner signer = new JWTSigner(jwtSecret);

        final HashMap<String, Object> claims = new HashMap<>();
        claims.put("vt", 1); //version=1 & type=access_token
        claims.put("exp", exp);
        claims.put("h", hash);
        claims.put("grant", grant_id);
        final String token_a = signer.sign(claims);

        final HashMap<String, Object> claims_r = new HashMap<>();
        final long exp_r = iat + expireRefreshToken; // refresh token expire time: 1 week
        claims_r.put("vt", 2); //version=1 & type=refresh_token
        claims_r.put("exp", exp_r);
        claims_r.put("h", hash);
        claims_r.put("grant", grant_id);
        final String token_r = signer.sign(claims_r);

        /* The last parameter (scope) is entirely optional. You can use it to implement scoping requirements. If you would like so, put it to claims map to verify it. */
        return new Token(token_a, token_r, "bearer", expireAccessToken, scopes == null ? "" : String.join(" ", scopes));
    }

    /**
     * Validate given token and return its type
     * @see TokenStatus
     */
    public Tuple2<Grant, TokenStatus> getTokenStatus(String access_token){
        if(access_token == null)
            return new Tuple2<>(null, TokenStatus.INVALID);
        try {
            final JWTVerifier verifier = new JWTVerifier(jwtSecret);
            final Map<String,Object> claims = verifier.verify(access_token);
            // first check version
            int type = (int) claims.get("vt");
            if(type != 1 && type != 2){
                return new Tuple2<>(null, TokenStatus.INVALID);
            }
            // check expiry date
            int exp = (int) claims.get("exp");
            if(exp < (System.currentTimeMillis() / 1000L))
                return new Tuple2<>(null, TokenStatus.INVALID);
            // check grant
            String grant_id = (String) claims.get("grant");
            Grant grant = grantRepository.findById(grant_id);
            if(grant == null){
                return new Tuple2<>(null, TokenStatus.INVALID);
            }
            Client client = clientRepository.findById(grant.getClientId());
            if(client == null){
                return new Tuple2<>(null, TokenStatus.INVALID); // how can this be?
            }
            // check hash value
            int receivedHash = (int) claims.get("h");
            int correctHash = Objects.hash(client.getId(), client.getSecret());
            if(receivedHash != correctHash) {
                return new Tuple2<>(null, TokenStatus.INVALID);
            }
            // check token type & version
            if(type == 1){
                return new Tuple2<>(grant, TokenStatus.VALID_ACCESS);
            } else {
                return new Tuple2<>(grant, TokenStatus.VALID_REFRESH);
            }
        } catch (JWTVerifyException | SignatureException | InvalidKeyException | NullPointerException e) {
            return new Tuple2<>(null, TokenStatus.INVALID);
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
            return new Tuple2<>(null, TokenStatus.INVALID);
        }
    }

}