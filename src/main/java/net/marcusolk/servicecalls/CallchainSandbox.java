package net.marcusolk.servicecalls;

import reactor.core.publisher.Mono;

import static java.lang.System.out;

class CallchainSandbox {

	/* The following methods shall simulate a real service call like the following

	Mono<Result> callService1(Params params) {
		return client.post()
			.body(fromObject(params))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(
					HttpStatus.PRECONDITION_FAILED::equals,
					response -> Mono.empty())
				.onStatus(
					HttpStatus::isError,
					response -> Mono.error(new RuntimeException("Calling service failed with status: " + response.statusCode())
				.bodyToMono(Result.class);
	}

	*/

	static Mono<Result1> callService1(Params params) {
		out.println("callService1 called with " + params);

		return params.value
			? Mono.just(new Result1(42))
			: Mono.empty();
	}

	// pass result of service1 call to service2 giving us result2
	static Mono<Result2> callService2(Result1 param) {
		out.println("callService2 called with " + param);

		int value = param == null || param == Result1.EMPTY ? 1 : param.result + 1;

		return Mono.just(new Result2(value));
	}

	public static void main(String[] args) {

		Integer result =

			callService1(new Params(false))
				// some side effect code when call was successful,
				// regardless of the Mono cardinality (0 or 1)
				.doOnSuccess( callService1Result -> out.println("callServicedoOnSuccess: " + callService1Result) )
				.defaultIfEmpty(Result1.EMPTY)

				// the call chain stops here in case of an Mono.empty() as a call result
				// so flatMap can't be used here
				// Mono.then() does not help, because we want to receive
				// callService1Result regardless of its Mono cardinality (0 or 1)
				.flatMap(callService1Result -> callService2(callService1Result))

				// ... more services calls following finally providing the overall call chain result
				.flatMap(callService2Result -> Mono.just(callService2Result.result))

				// block for testing purposes
				.block();

		out.println(result);
	}

	//~ demo classes ---------------------------------------------------------------------------

	static class Params {
		boolean value;
		Params(boolean value) { this.value = value; }
		public String toString() { return "Params: " + value; }
	}

	static class Result1 {

		// artificial EMPTY result
		static final Result1 EMPTY = new EmptyResult1() {
			@Override
			public String toString() {
				return "EmptyResult1";
			}
		};
		static class EmptyResult1 extends Result1 {}

		Integer result;
		private Result1() { result = null; }
		Result1(int result) { this.result = result; }
		public String toString() { return "Result1: " + result; }
	}

	static class Result2 {
		int result;
		Result2(int result) { this.result = result; }
		public String toString() { return "Result1: " + result; }
	}

}
