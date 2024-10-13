package telran.monitoring.pulse;
import java.net.*;
import java.util.*;
import java.util.stream.IntStream;

import telran.monitoring.pulse.dto.SensorData;
public class PulseSenderAppl {
	private static final int N_PACKETS = 200;
	private static final long TIMEOUT = 500;
	private static final int N_PATIENTS = 5;
	private static final int MIN_PULSE_VALUE = 50;
	private static final int MAX_PULSE_VALUE = 200;
	private static final int JUMP_PROBABILITY = 15;
	private static final int JUMP_POSITIVE_PROBABILITY=70;
	private static final int MIN_JUMP_PERCENT=10;
	private static final int MAX_JUMP_PERCENT=90;
	private static final int MAX_NEGATIVE_JUMP_PERCENT = 50;
	private static final String HOST = "localhost";
	private static final int PORT = 5000;
	private static Random random = new Random();
	static DatagramSocket socket;
	static Map<Long, Integer> previousValuePulse=new HashMap<>();
	public static void main(String[] args) throws Exception{
		socket = new DatagramSocket();
		IntStream.rangeClosed(1, N_PACKETS)
		.forEach(PulseSenderAppl::sendPulse);

	}
	static void sendPulse(int seqNumber) {
		SensorData data = getRandomSensorData(seqNumber);
		String jsonData = data.toString();
		sendDatagramPacket(jsonData);
		try {
			Thread.sleep(TIMEOUT);
		} catch (InterruptedException e) {
			
		}
	}
	private static void sendDatagramPacket(String jsonData) {
		byte [] buffer = jsonData.getBytes();
		try {
			DatagramPacket packet =
					new DatagramPacket(buffer, buffer.length,
							InetAddress.getByName(HOST), PORT);
			socket.send(packet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	private static SensorData getRandomSensorData(int seqNumber) {
		
		long patientId = random.nextInt(1, N_PATIENTS + 1);
		int value = getRandomPulseValue(patientId);
		return new SensorData(seqNumber, patientId, value, System.currentTimeMillis());
	}
	private static int getRandomPulseValue(long patientId) {
		int previosValue = previousValuePulse.getOrDefault(patientId, -1);
		int result = (previosValue == -1) ? generateRandomPulse() : processPulseWithJump(previosValue);
		previousValuePulse.put(patientId, result);

		return result;
	}
	private static int processPulseWithJump(int prevValue) {
		int newValue = prevValue;
		if (isEventHappening(JUMP_PROBABILITY)) {
			int sign = isEventHappening(JUMP_POSITIVE_PROBABILITY) ? 1 : -1;
			int maxJumpPercent = (sign == -1) ? MAX_NEGATIVE_JUMP_PERCENT : MAX_JUMP_PERCENT;
			int jumpPercent = random.nextInt(MIN_JUMP_PERCENT, maxJumpPercent);
			int delta = prevValue * jumpPercent / 100;

			 newValue = prevValue + (sign * delta);
			newValue = clampPulseValue(newValue);
		}
		return newValue;
	}
	private static int clampPulseValue(int newValue) {
		return Math.max(MIN_PULSE_VALUE, Math.min(MAX_PULSE_VALUE, newValue));
	}
	private static boolean isEventHappening(int jumpProbability) {
		return random.nextInt(100) < jumpProbability;
	}
	private static int generateRandomPulse() {
		return random.nextInt(MIN_PULSE_VALUE, MAX_PULSE_VALUE + 1);
	}

}
