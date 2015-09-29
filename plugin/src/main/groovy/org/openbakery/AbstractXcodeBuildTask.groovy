package org.openbakery

import org.apache.commons.io.input.ReversedLinesFileReader
import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.openbakery.signing.ProvisioningProfileReader

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
abstract class AbstractXcodeBuildTask extends DefaultTask {

	CommandRunner commandRunner

	AbstractXcodeBuildTask() {
		super()
		commandRunner = new CommandRunner()
	}

	def createCommandList() {

		def commandList = [
					project.xcodebuild.xcodebuildCommand
		]

		if (project.xcodebuild.scheme) {
			commandList.add("-scheme");
			commandList.add(project.xcodebuild.scheme);

			if (project.xcodebuild.workspace != null) {
				commandList.add("-workspace")
				commandList.add(project.xcodebuild.workspace)
			}

			if (project.xcodebuild.sdk != null) {
				commandList.add("-sdk")
				commandList.add(project.xcodebuild.sdk)
				if (project.xcodebuild.sdk.equals(XcodePlugin.SDK_IPHONESIMULATOR) && project.xcodebuild.arch != null) {
					commandList.add("ONLY_ACTIVE_ARCH=NO")
				}
			}

			if (project.xcodebuild.configuration != null) {
				commandList.add("-configuration")
				commandList.add(project.xcodebuild.configuration)
			}


		} else {
			commandList.add("-configuration")
			commandList.add(project.xcodebuild.configuration)
			commandList.add("-sdk")
			commandList.add(project.xcodebuild.sdk)
			commandList.add("-target")
			commandList.add(project.xcodebuild.target)
		}

		if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONEOS)) {
			if (project.xcodebuild.signing != null && StringUtils.isNotEmpty(project.xcodebuild.signing.identity)) {
				commandList.add("CODE_SIGN_IDENTITY=" + project.xcodebuild.signing.identity)
				if (project.xcodebuild.signing.mobileProvisionFile.size() == 1) {
					ProvisioningProfileReader provisioningProfileIdReader = new ProvisioningProfileReader(project.xcodebuild.signing.mobileProvisionFile.get(0), project)
					String uuid = provisioningProfileIdReader.getUUID()
					commandList.add("PROVISIONING_PROFILE=" + uuid)
				}
			} else {
				commandList.add("CODE_SIGN_IDENTITY=")
				commandList.add("CODE_SIGNING_REQUIRED=NO")
			}
		} else if (project.xcodebuild.isSDK(XcodePlugin.SDK_MACOSX)) {
			// disable signing during xcodebuild for os x, maybe this should be also default for iOS?
			commandList.add("CODE_SIGN_IDENTITY=")
			commandList.add("CODE_SIGNING_REQUIRED=NO")

		}



		if (project.xcodebuild.arch != null) {
			StringBuilder archs = new StringBuilder("ARCHS=");
			for (String singleArch in project.xcodebuild.arch) {
				if (archs.length() > 7) {
					archs.append(" ");
				}
				archs.append(singleArch);
			}
			commandList.add(archs.toString());

		}

		commandList.add("-derivedDataPath")
		commandList.add(project.xcodebuild.derivedDataPath.absolutePath)
		commandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
		commandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
		commandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)


		if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONEOS) && project.xcodebuild.signing.keychainPathInternal.exists()) {
			commandList.add('OTHER_CODE_SIGN_FLAGS=--keychain=' + project.xcodebuild.signing.keychainPathInternal.path)
		}


		if (project.xcodebuild.additionalParameters instanceof List) {
			for (String value in project.xcodebuild.additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (project.xcodebuild.additionalParameters != null) {
				commandList.add(project.xcodebuild.additionalParameters)
			}
		}

		return commandList;
	}


	String getFailureFromLog(File outputFile) {

		ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(outputFile);

		ArrayList<String> result = new ArrayList<>(100);

		for (int i=0; i<100; i++) {
			String line = reversedLinesFileReader.readLine()

			if (line == null) {
				// no more input so we are done;
				break;
			}

			result.add(line);

			if (line.startsWith("Testing failed:")) {
				break
			}

		}

		Collections.reverse(result)
		StringBuilder builder = new StringBuilder()
		for (String line : result) {
		  builder.append(line)
			builder.append("\n")
		}

		return builder.toString()
	}
}
