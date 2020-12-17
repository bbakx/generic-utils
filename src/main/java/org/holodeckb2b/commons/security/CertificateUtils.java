/*******************************************************************************
 * Copyright (C) 2020 The Holodeck Team, Sander Fieten
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.holodeckb2b.commons.security;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.util.encoders.Base64;
import org.holodeckb2b.commons.util.Utils;

/**
 * Is a utility class for processing of X509 certificates.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class CertificateUtils {
	/**
	 * The start boundary of a PEM formatted certificate 
	 */
    private static final String PEM_START_BOUNDARY = "-----BEGIN CERTIFICATE-----";
    
	/**
     * Certificate factory to create the X509 Certificate object
     */
    private static CertificateFactory certificateFactory;

    /**
     * Gets the X509 Certificate from the base64 encoded DER byte array which may be PEM encapsulated.   
     *
     * @param b64EncodedCertificate The string containing the base64 encoded bytes
     * @return  The decoded Certificate instance, <code>null</code> if the given string is <code>null</code> or empty
     * @throws CertificateException When the string does not contain a valid base64 encoded certificate or when there is
     *                              a problem in loading required classes for certificate processing
     */
    public static X509Certificate getCertificate(final String b64EncodedCertificate) throws CertificateException {
    	if (Utils.isNullOrEmpty(b64EncodedCertificate))
    		return null;
    	// Strip everything up to first and after the last possible PEM boundaries 
    	final String encodedBytes = b64EncodedCertificate.contains(PEM_START_BOUNDARY) ?
    			b64EncodedCertificate.substring(b64EncodedCertificate.indexOf('\n', 
    																b64EncodedCertificate.indexOf(PEM_START_BOUNDARY))    											  
    											, b64EncodedCertificate.length() - 25)
    									: b64EncodedCertificate;
    	
    	byte[] certBytes = null;
    	try {
    		certBytes = Base64.decode(encodedBytes);
    	} catch (Exception decodingFailure) {
    		throw new CertificateException("String is not a valid base64 encoding", decodingFailure);    		
    	}
    	
        return getCertificate(certBytes);
    }

    /**
     * Gets the DER encoded X509 Certificate from the specified file. The format of the file can be both the plain 
     * binary DER or PEM (base64 encoded) format. 
     *
     * @param b64EncodedCertificate Path to the file containing the certificate, must not be <code>null</code>
     * @return  The decoded Certificate instance
     * @throws CertificateException When an error occurs reading the certificate from the file. For example when the 
     * 								file can not be read or does not contain a valid certificate.
     */    
    public static X509Certificate getCertificate(final Path certificateFile) throws CertificateException {
    	if (certificateFile == null)
    		throw new IllegalArgumentException("A path must be specified");
    	try(FileInputStream fis = new FileInputStream(certificateFile.toFile())) {
    		return (X509Certificate) getCertificateFactory().generateCertificate(fis);
    	} catch (IOException e) {
    		throw new CertificateException("Error accessing certificate file");
    	}
    }
    
    /**
     * Converts the given byte array into a X509 Certificate.
     *
     * @param certBytes The byte array containing the encoded certificate
     * @return  The Certificate instance
     * @throws CertificateException When there is a problem in loading required classes for certificate processing
     */
    public static X509Certificate getCertificate(final byte[] certBytes) throws CertificateException {
        if (certBytes == null || certBytes.length == 0)
            return null;
        else
        	return (X509Certificate) getCertificateFactory().generateCertificate(new ByteArrayInputStream(certBytes));
    }
    
    /**
     * Gets the CN field of the provided X509 certificate's Subject DN.
     * 
     * @param cert	The X509 certificate
     * @return	The CN field of the Subject's DN 
     * @throws CertificateException When the certificate could not be read
     */
    public static String getSubjectCN(final X509Certificate cert) {
		X500Name x500name = new X500Name(cert.getSubjectX500Principal().getName());
		RDN cn = x500name.getRDNs(BCStyle.CN)[0];
		return IETFUtils.valueToString(cn.getFirst().getValue());
    }

    /**
     * Gets the Serial Number field of the provided X509 certificate's Subject DN. 
     * <p>NOTE: This is a different serial number then the serial number of the certificate itself! 
     * 
     * @param cert	The X509 certificate
     * @return	The Subject's Serial Number field 
     * @throws CertificateException When the certificate could not be read
     */
    public static String getSubjectSN(final X509Certificate cert)  {
    	try {
    		X500Name x500name = new X500Name(cert.getSubjectX500Principal().getName());
    		RDN cn = x500name.getRDNs(BCStyle.SN)[0];
    		return IETFUtils.valueToString(cn.getFirst().getValue());
    	} catch (Exception invalidCert) {
    		return null;
    	}    	
    }
    
    /**
     * Gets the {@link CertificateFactory} instance to use for creating the <code>X509Certificate</code> object from a
     * byte array.
     *
     * @return  The <code>CertificateFactory</code> to use
     * @throws CertificateException     When the certificate factory could not be loaded
     */
    private static CertificateFactory getCertificateFactory() throws CertificateException {
        if (certificateFactory == null)
            certificateFactory = CertificateFactory.getInstance("X.509");
        return certificateFactory;
    }
}
