package com.monkeybody.brain.api;

/** Monkey Face 对接门面。宿主在 Application 初始化时注册实现即可。 */
public final class MonkeyBrainBridge {
    private static volatile ScreenCaptureController captureController;
    private static volatile RecognitionResultReceiver resultReceiver;
    private static volatile FeedbackOutput feedbackOutput;
    private static volatile ScreenFrameConsumer frameConsumer;
    private MonkeyBrainBridge() {}

    public static void register(ScreenCaptureController capture, RecognitionResultReceiver receiver, FeedbackOutput feedback) {
        captureController = capture;
        resultReceiver = receiver;
        feedbackOutput = feedback;
    }
    public static ScreenCaptureController capture() { return captureController; }
    public static RecognitionResultReceiver receiver() { return resultReceiver; }
    public static FeedbackOutput feedback() { return feedbackOutput; }
    public static void registerCaptureController(ScreenCaptureController controller) { captureController = controller; }
    public static void registerFeedbackOutput(FeedbackOutput output) { feedbackOutput = output; }
    public static void registerRecognitionReceiver(RecognitionResultReceiver receiver) { resultReceiver = receiver; }
    public static void clearCaptureController(ScreenCaptureController controller) {
        if (captureController == controller) captureController = null;
    }
    public static void clearFeedbackOutput(FeedbackOutput output) {
        if (feedbackOutput == output) feedbackOutput = null;
    }
    public static void clearRecognitionReceiver(RecognitionResultReceiver receiver) {
        if (resultReceiver == receiver) resultReceiver = null;
    }
    public static void registerFrameConsumer(ScreenFrameConsumer consumer) { frameConsumer = consumer; }
    public static ScreenFrameConsumer frameConsumer() { return frameConsumer; }
    public static void clear() { captureController = null; resultReceiver = null; feedbackOutput = null; frameConsumer = null; }
}
